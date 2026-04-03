'use client';

import { useState, useEffect, useCallback, useRef, use, useMemo } from 'react';
import Link from 'next/link';
import { leaderboardApi, contestApi } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { connectWebSocket } from '@/lib/websocket';
import type { LeaderboardEntry, LeaderboardUpdate } from '@/lib/types';
import { toast } from '@/app/components/Toast';
import {
  ArrowLeft,
  BarChart3,
  Trophy,
  Wifi,
  WifiOff,
  RefreshCw,
  Medal,
  TrendingUp,
  Clock,
  Sparkles,
  Bot,
} from 'lucide-react';
import styles from './leaderboard.module.css';

export default function LeaderboardPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id: contestId } = use(params);
  const { user, isAuthenticated } = useAuth();
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [wsConnected, setWsConnected] = useState(false);
  const [flashMap, setFlashMap] = useState<Record<string, 'ac' | 'wa'>>({});
  const disconnectRef = useRef<(() => void) | null>(null);
  const [allowed, setAllowed] = useState(false);
  const [isOwner, setIsOwner] = useState(false);
  const [contestTitle, setContestTitle] = useState('Contest');
  const [aiAnalysis, setAiAnalysis] = useState('');
  const [aiLoading, setAiLoading] = useState(false);
  const [lastAnalyzedAt, setLastAnalyzedAt] = useState<string | null>(null);

  // Access guard: admin, creator, or registered participant
  useEffect(() => {
    const guard = async () => {
      try {
        const contest = await contestApi.getContest(contestId);
        const isCreator = user?.userId && contest.createdBy === user.userId;
        const isAdmin = user?.role === 'ADMIN';
        const ownerAccess = Boolean(isCreator || isAdmin);
        const isParticipant = contest.registered === true;
        setContestTitle(contest.title || 'Contest');
        setIsOwner(ownerAccess);
        if (isCreator || isAdmin || isParticipant) {
          setAllowed(true);
        } else {
          setAllowed(false);
          toast.error('Leaderboard visible only to creator, participants, or admins');
        }
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Failed to verify access';
        toast.error(message);
      }
    };
    guard();
  }, [contestId, user]);

  const fetchLeaderboard = useCallback(async () => {
    try {
      const data = await leaderboardApi.getLeaderboard(contestId);
      setEntries(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load leaderboard';
      toast.error(message);
    } finally {
      setLoading(false);
    }
  }, [contestId]);

  // Initial load
  useEffect(() => {
    if (allowed) {
      fetchLeaderboard();
    }
  }, [allowed, fetchLeaderboard]);

  // WebSocket connection
  useEffect(() => {
    if (!allowed) return;
    const disconnect = connectWebSocket(
      contestId,
      (update: LeaderboardUpdate) => {
        // Flash the row
        setFlashMap((prev) => ({
          ...prev,
          [update.userId]: update.verdict === 'AC' ? 'ac' : 'wa',
        }));
        // Clear flash after animation
        setTimeout(() => {
          setFlashMap((prev) => {
            const next = { ...prev };
            delete next[update.userId];
            return next;
          });
        }, 1500);

        // Update or insert entry
        setEntries((prev) => {
          const existing = prev.find((e) => e.userId === update.userId);
          const normalizeRanks = (list: LeaderboardEntry[]): LeaderboardEntry[] => {
            const sorted = [...list].sort((a, b) => {
              if (b.totalScore !== a.totalScore) return b.totalScore - a.totalScore;
              if (a.totalPenalty !== b.totalPenalty) return a.totalPenalty - b.totalPenalty;
              if (b.solvedCount !== a.solvedCount) return b.solvedCount - a.solvedCount;
              return a.username.localeCompare(b.username);
            });
            return sorted.map((entry, idx) => ({ ...entry, rank: idx + 1 }));
          };

          if (existing) {
            const updated = prev.map((e) =>
              e.userId === update.userId
                ? {
                    ...e,
                    solvedCount: update.solvedCount,
                    totalScore: update.totalScore,
                    totalPenalty: update.totalPenalty,
                    lastAcAt: update.verdict === 'AC' ? new Date().toISOString() : e.lastAcAt,
                  }
                : e,
            );
            return normalizeRanks(updated);
          } else {
            return normalizeRanks([
              ...prev,
              {
                userId: update.userId,
                username: update.username,
                solvedCount: update.solvedCount,
                totalScore: update.totalScore,
                totalPenalty: update.totalPenalty,
                rank: 0,
                lastAcAt: update.verdict === 'AC' ? new Date().toISOString() : null,
              },
            ]);
          }
        });
      },
      () => setWsConnected(true),
      () => setWsConnected(false),
    );

    disconnectRef.current = disconnect;
    return () => disconnect();
  }, [contestId, allowed]);

  const stats = useMemo(() => {
    const participants = entries.length;
    const totalAccepted = entries.reduce((sum, e) => sum + e.solvedCount, 0);
    const totalScore = entries.reduce((sum, e) => sum + e.totalScore, 0);
    const totalPenalty = entries.reduce((sum, e) => sum + e.totalPenalty, 0);
    const averageSolved = participants ? Number((totalAccepted / participants).toFixed(2)) : 0;
    const averageScore = participants ? Number((totalScore / participants).toFixed(2)) : 0;
    const averagePenalty = participants ? Number((totalPenalty / participants).toFixed(2)) : 0;
    const top3 = [...entries]
      .sort((a, b) => {
        if (b.totalScore !== a.totalScore) return b.totalScore - a.totalScore;
        if (a.totalPenalty !== b.totalPenalty) return a.totalPenalty - b.totalPenalty;
        if (b.solvedCount !== a.solvedCount) return b.solvedCount - a.solvedCount;
        return a.username.localeCompare(b.username);
      })
      .slice(0, 3);

    return {
      participants,
      totalAccepted,
      totalScore,
      totalPenalty,
      averageSolved,
      averageScore,
      averagePenalty,
      top3,
      totalSubmissions: null as number | null,
    };
  }, [entries]);

  const runAiAnalysis = useCallback(async () => {
    if (!isOwner) return;

    const apiKey = process.env.NEXT_PUBLIC_GEMINI_API_KEY;
    if (!apiKey) {
      toast.error('Missing NEXT_PUBLIC_GEMINI_API_KEY in frontend env.');
      return;
    }

    if (entries.length === 0) {
      toast.error('No leaderboard data available to analyze.');
      return;
    }

    setAiLoading(true);
    setAiAnalysis('');

    const payload = {
      contestId,
      contestTitle,
      generatedAt: new Date().toISOString(),
      participants: stats.participants,
      totalAccepted: stats.totalAccepted,
      totalScore: stats.totalScore,
      totalPenalty: stats.totalPenalty,
      averageSolved: stats.averageSolved,
      averageScore: stats.averageScore,
      averagePenalty: stats.averagePenalty,
      totalSubmissions: stats.totalSubmissions,
      submissionHistoryAvailable: false,
      top3: stats.top3.map((e) => ({
        rank: e.rank,
        username: e.username,
        solved: e.solvedCount,
        score: e.totalScore,
        penalty: e.totalPenalty,
        lastAcAt: e.lastAcAt,
      })),
      firstPerformer: stats.top3[0]
        ? {
            rank: stats.top3[0].rank,
            username: stats.top3[0].username,
            solved: stats.top3[0].solvedCount,
            score: stats.top3[0].totalScore,
            penalty: stats.top3[0].totalPenalty,
            lastAcAt: stats.top3[0].lastAcAt,
          }
        : null,
      rankGaps: {
        scoreGapFirstVsSecond:
          stats.top3.length >= 2 ? stats.top3[0].totalScore - stats.top3[1].totalScore : null,
        penaltyGapFirstVsSecond:
          stats.top3.length >= 2 ? stats.top3[1].totalPenalty - stats.top3[0].totalPenalty : null,
      },
      fullLeaderboard: entries.map((e) => ({
        rank: e.rank,
        username: e.username,
        solved: e.solvedCount,
        score: e.totalScore,
        penalty: e.totalPenalty,
        lastAcAt: e.lastAcAt,
        scorePerSolved: e.solvedCount > 0 ? Number((e.totalScore / e.solvedCount).toFixed(2)) : 0,
        penaltyPerSolved: e.solvedCount > 0 ? Number((e.totalPenalty / e.solvedCount).toFixed(2)) : 0,
      })),
      leaderboardJson: entries,
    };

    const prompt = [
      'You are an expert competitive-programming contest analyst.',
      'Analyze this leaderboard snapshot and produce a concise owner report in 5 to 10 lines only.',
      'Use only given data. If a metric is unavailable (e.g., totalSubmissions is null or submissionHistoryAvailable=false), explicitly mark it as Not Available.',
      'The raw leaderboard JSON is included as leaderboardJson. Use it as the primary source.',
      'Include top 3 performers (with usernames) and explain why rank #1 is leading.',
      'Mention the key metrics considered: solved, score, penalty, efficiency, and any notable risk/opportunity.',
      'Do not output section headings or bullet points. Write plain short lines only.',
      'Keep output compact, numeric, and decision-oriented.',
      '',
      JSON.stringify(payload),
    ].join('\n');

    try {
      const preferredModel = process.env.NEXT_PUBLIC_GEMINI_MODEL;
      let discoveredModels: string[] = [];

      try {
        const listResp = await fetch(
          `https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`,
        );
        if (listResp.ok) {
          const listData = await listResp.json();
          const modelsFromApi: Array<{ name?: string; supportedGenerationMethods?: string[] }> =
            listData?.models || [];

          discoveredModels = modelsFromApi
            .filter((m) => (m.supportedGenerationMethods || []).includes('generateContent'))
            .map((m) => (m.name || '').replace(/^models\//, ''))
            .filter((name) =>
              name.includes('flash') &&
              !name.includes('vision') &&
              !name.includes('embedding')
            );
        }
      } catch {
        // Ignore model-list failures; static fallback still applies.
      }

      const staticFallbackModels = [
        'gemini-2.0-flash',
        'gemini-1.5-flash-latest',
      ];

      const models = Array.from(
        new Set([
          ...(preferredModel ? [preferredModel] : []),
          ...discoveredModels,
          ...staticFallbackModels,
        ]),
      );

      let data: any = null;
      let lastError = '';

      for (const model of models) {
        const response = await fetch(
          `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              contents: [
                {
                  parts: [{ text: prompt }],
                },
              ],
              generationConfig: {
                temperature: 0.4,
                maxOutputTokens: 900,
              },
            }),
          },
        );

        if (response.ok) {
          data = await response.json();
          break;
        }

        const msg = await response.text();
        lastError = `${response.status} (${model}) ${msg}`;
      }

      if (!data) {
        throw new Error(`Gemini API error: ${lastError}. Tried models: ${models.join(', ')}.`);
      }

      const text = (data?.candidates?.[0]?.content?.parts || [])
        .map((part: { text?: string }) => part?.text || '')
        .join('\n')
        .trim();

      if (!text) {
        throw new Error('Gemini returned empty analysis.');
      }

      setAiAnalysis(text);
      setLastAnalyzedAt(new Date().toLocaleTimeString());
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'AI analysis failed';
      toast.error(message);
      setAiAnalysis('');
    } finally {
      setAiLoading(false);
    }
  }, [contestId, contestTitle, entries, isOwner, stats]);

  const getRankIcon = (rank: number) => {
    if (rank === 1) return <Medal size={18} style={{ color: '#FFD700' }} />;
    if (rank === 2) return <Medal size={18} style={{ color: '#C0C0C0' }} />;
    if (rank === 3) return <Medal size={18} style={{ color: '#CD7F32' }} />;
    return <span className={styles.rankNumber}>{rank}</span>;
  };

  if (!allowed) {
    return (
      <div className={`container ${styles.page}`}>
        <Link href={`/contests/${contestId}`} className={styles.back} id="leaderboard-back">
          <ArrowLeft size={16} />
          Back to Contest
        </Link>
        <div className={styles.loading}>
          <p>Access restricted to creator, participants, or admins.</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`container ${styles.page}`}>
      <Link href={`/contests/${contestId}`} className={styles.back} id="leaderboard-back">
        <ArrowLeft size={16} />
        Back to Contest
      </Link>

      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <h1>
            <BarChart3 size={32} className={styles.titleIcon} />
            Leaderboard
          </h1>
          <div className={styles.wsStatus}>
            {wsConnected ? (
              <>
                <Wifi size={14} className={styles.wsOnline} />
                <span className={styles.wsText}>Live</span>
              </>
            ) : (
              <>
                <WifiOff size={14} className={styles.wsOffline} />
                <span className={styles.wsText}>Offline</span>
              </>
            )}
          </div>
        </div>

        <div className={styles.headerActions}>
          {isOwner && (
            <button
              onClick={runAiAnalysis}
              className="btn btn-primary"
              id="leaderboard-ai-analysis"
              disabled={aiLoading || entries.length === 0}
            >
              <Sparkles size={16} />
              {aiLoading ? 'Analyzing...' : 'AI Analysis'}
            </button>
          )}
          <button onClick={fetchLeaderboard} className="btn btn-secondary" id="leaderboard-refresh">
            <RefreshCw size={16} />
            Refresh
          </button>
        </div>
      </div>

      {isOwner && (
        <section className={styles.analysisPanel}>
          <div className={styles.analysisHeader}>
            <h3 className={styles.analysisTitle}>
              <Bot size={18} /> Owner AI Insights
            </h3>
            <span className={styles.analysisMeta}>
              {lastAnalyzedAt ? `Last analyzed at ${lastAnalyzedAt}` : 'No analysis yet'}
            </span>
          </div>

          <div className={styles.statsGrid}>
            <div className={styles.statCard}>
              <span>Participants</span>
              <strong>{stats.participants}</strong>
            </div>
            <div className={styles.statCard}>
              <span>Total Accepted</span>
              <strong>{stats.totalAccepted}</strong>
            </div>
            <div className={styles.statCard}>
              <span>Avg Solved</span>
              <strong>{stats.averageSolved}</strong>
            </div>
            <div className={styles.statCard}>
              <span>Total Submissions</span>
              <strong>N/A</strong>
            </div>
          </div>

          <div className={styles.topPerformersBox}>
            <h4 className={styles.topPerformersTitle}>Top Performers Overview</h4>
            {stats.top3.length === 0 ? (
              <p className={styles.topPerformersEmpty}>No ranked users yet.</p>
            ) : (
              <div className={styles.topPerformersList}>
                {stats.top3.map((p, idx) => (
                  <div key={p.userId} className={styles.topPerformerItem}>
                    <div className={styles.topPerformerRank}>#{idx + 1}</div>
                    <div className={styles.topPerformerMeta}>
                      <strong>{p.username}</strong>
                      <span>
                        Solved: {p.solvedCount} | Score: {p.totalScore} | Penalty: {p.totalPenalty}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {stats.top3[0] && (
              <div className={styles.firstPerformerBox}>
                <h5>First Performer Detail</h5>
                <p>
                  <strong>{stats.top3[0].username}</strong> leads with score <strong>{stats.top3[0].totalScore}</strong>,
                  solved <strong>{stats.top3[0].solvedCount}</strong>, and penalty <strong>{stats.top3[0].totalPenalty}</strong>.
                </p>
              </div>
            )}
          </div>

          {aiAnalysis && (
            <pre className={styles.analysisText}>{aiAnalysis}</pre>
          )}
        </section>
      )}

      {loading ? (
        <div className={styles.loading}>
          <div className={styles.spinner} />
          <p>Loading leaderboard...</p>
        </div>
      ) : entries.length === 0 ? (
        <div className={styles.empty}>
          <Trophy size={48} className={styles.emptyIcon} />
          <h3>No entries yet</h3>
          <p>The leaderboard will populate once participants submit solutions.</p>
        </div>
      ) : (
        <div className={styles.table}>
          <div className={styles.tableHeader}>
            <div className={styles.colRank}>Rank</div>
            <div className={styles.colUser}>User</div>
            <div className={styles.colSolved}>Solved</div>
            <div className={styles.colScore}>Score</div>
            <div className={styles.colPenalty}>Penalty</div>
            <div className={styles.colLastAc}>Last AC</div>
          </div>

          <div className={styles.tableBody}>
            {entries.map((entry, index) => {
              const flash = flashMap[entry.userId];
              return (
                <div
                  key={entry.userId}
                  className={`${styles.row} ${
                    flash === 'ac' ? styles.flashAc : ''
                  } ${flash === 'wa' ? styles.flashWa : ''} ${
                    index < 3 ? styles.topThree : ''
                  }`}
                  style={{
                    animationDelay: `${index * 50}ms`,
                    animationFillMode: 'both',
                  }}
                >
                  <div className={styles.colRank}>{getRankIcon(entry.rank)}</div>
                  <div className={styles.colUser}>
                    <div className={styles.userAvatar}>
                      {entry.username.charAt(0).toUpperCase()}
                    </div>
                    <span className={styles.username}>{entry.username}</span>
                  </div>
                  <div className={`${styles.colSolved} mono`}>{entry.solvedCount}</div>
                  <div className={`${styles.colScore} mono`}>
                    <TrendingUp size={14} />
                    {entry.totalScore}
                  </div>
                  <div className={`${styles.colPenalty} mono`}>
                    <Clock size={14} />
                    {entry.totalPenalty}
                  </div>
                  <div className={`${styles.colLastAc} mono`}>
                    {entry.lastAcAt
                      ? new Date(entry.lastAcAt).toLocaleTimeString()
                      : '—'}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
