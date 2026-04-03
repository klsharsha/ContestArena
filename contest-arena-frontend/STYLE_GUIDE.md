# ContestArena UI/UX Style Guide

## 🎨 Design System Overview

ContestArena features a **premium dark-mode-first design system** with glassmorphism, cyberpunk aesthetics, and smooth animations. This guide ensures consistency across all pages and components.

---

## 🎯 Color Palette

### Primary Colors
- **Cyan**: `#00d4ff` - Primary actions, active states, hover effects
- **Violet**: `#8b5cf6` - Secondary accents, featured elements
- **Green**: `#22c55e` - Success, AC verdicts, accepted submissions
- **Red**: `#ef4444` - Errors, WA verdicts, danger states
- **Amber**: `#f59e0b` - Warnings, pending states, alerts

### Backgrounds
- **Primary**: `#0a0a0f` - Main background
- **Secondary**: `#12121a` - Cards, panels
- **Tertiary**: `#1a1a2e` - Elevated surfaces
- **Elevated**: `#1e1e30` - Highest level surfaces

### Text
- **Primary**: `#e8e8f0` - Main text
- **Secondary**: `#8888a0` - Descriptive text
- **Tertiary**: `#5a5a72` - Subtle text

---

## ✨ Component Styles

### Buttons

#### Primary Button
```html
<button class="btn btn-primary">Action</button>
```
- **Style**: Gradient cyan-to-violet, glowing shadow on hover
- **Use**: Main CTAs, form submissions, important actions

#### Secondary Button
```html
<button class="btn btn-secondary">Secondary</button>
```
- **Style**: Glass effect, border highlight on hover
- **Use**: Alternative actions, less critical flows

#### Ghost Button
```html
<button class="btn btn-ghost">Ghost</button>
```
- **Style**: Minimal, subtle background on hover
- **Use**: Tertiary actions, navigation

#### Danger Button
```html
<button class="btn btn-danger">Delete</button>
```
- **Style**: Red background with glow effect
- **Use**: Destructive actions only

#### Size Variants
- `btn-lg` - Large buttons for hero sections
- `btn-sm` - Small buttons for compact layouts
- `btn-icon` - Icon-only buttons

### Cards

#### Glass Card (Default)
```html
<div class="card card-hover">
  <!-- Content -->
</div>
```
- **Background**: Glassmorphic with blur effect
- **Border**: Subtle cyan glow on hover
- **Animation**: Smooth lift and shadow expansion
- **Use**: Contest cards, problem cards, content blocks

#### Elevated Card
```html
<div class="card card-elevated">
  <!-- Content -->
</div>
```
- **Background**: Solid secondary with shadow
- **Use**: More opaque, less prominent content

#### Outlined Card
```html
<div class="card card-outlined">
  <!-- Content -->
</div>
```
- **Background**: Transparent with border
- **Use**: Skeleton states, empty states

#### Accent Cards
```html
<div class="card card-accent-cyan"><!-- Cyan accent left border --></div>
<div class="card card-accent-violet"><!-- Violet accent --></div>
<div class="card card-accent-green"><!-- Green accent --></div>
<div class="card card-accent-red"><!-- Red accent --></div>
```

### Badges

#### Status Badges
```html
<span class="badge statusActive">Live</span>
<span class="badge statusScheduled">Upcoming</span>
<span class="badge statusEnded">Ended</span>
<span class="badge statusDraft">Draft</span>
```

#### Difficulty Badges
```html
<span class="badge difficultyEasy">Easy</span>
<span class="badge difficultyMedium">Medium</span>
<span class="badge difficultyHard">Hard</span>
```

#### Verdict Badges
```html
<span class="badge verdictAc">Accepted</span>
<span class="badge verdictWa">Wrong Answer</span>
<span class="badge verdictPending">Running</span>
```

### Form Elements

#### Input Fields
```html
<input class="input" type="text" placeholder="Enter text..." />
```
- **Style**: Dark background with cyan focus glow
- **Focus**: Blue glowing shadow and border highlight
- **Error**: Red border and glow

#### Selects and Dropdowns
```html
<select class="select">
  <option>Option 1</option>
</select>
```
- **Style**: Custom dropdown arrow, smooth transitions
- **Hover**: Border color change
- **Focus**: Cyan glow effect

#### Textarea
```html
<textarea class="input">Text content</textarea>
```
- **Resize**: Vertical only
- **Min Height**: 120px

#### Checkboxes & Radios
```html
<input type="checkbox" class="checkbox" />
<input type="radio" class="radio" />
```
- **Accent Color**: Cyan on focus

### Badges & Tags

#### Tag Component
```html
<div class="tag">Feature Tag</div>
<div class="tag tag-removable">
  Removable Tag
  <button>×</button>
</div>
```
- **Default**: Cyan background with subtle border
- **Hover**: Slightly brighter background
- **Variants**: `tag-sm` for smaller versions

---

## 🎬 Animations & Transitions

### Entrance Animations
```css
.animate-fade-in          /* Simple opacity fade */
.animate-fade-in-up       /* Slide up while fading */
.animate-fade-in-down     /* Slide down while fading */
.animate-slide-right      /* Slide in from left */
.animate-slide-left       /* Slide in from right */
.animate-scale-in         /* Scale from small */
```

### Stagger Effects
```css
.stagger-1, .stagger-2, .stagger-3, .stagger-4, .stagger-5
/* Apply with animation classes for cascade effect */
```

### Looping Animations
```css
.animate-pulse      /* Soft pulsing effect */
.animate-spin       /* Rotation for loading */
.animate-float      /* Floating up and down */
```

### Per-Component Timing
- **Fast transitions**: 150ms - Quick hovers, state changes
- **Base transitions**: 300ms - Default interactions
- **Slow transitions**: 500ms - Hero sections, page loads
- **Spring transitions**: 500ms - Bounce effects

---

## 📐 Layout & Spacing

### Container Classes
```html
<div class="container">
  <!-- Max-width: 1280px, centered, responsive padding -->
</div>

<div class="container-lg">
  <!-- Max-width: 1440px for wider layouts -->
</div>
```

### Spacing Scale
```
--space-xs   = 0.25rem    (4px)
--space-sm   = 0.5rem     (8px)
--space-md   = 1rem       (16px)
--space-lg   = 1.5rem     (24px)
--space-xl   = 2rem       (32px)
--space-2xl  = 3rem       (48px)
--space-3xl  = 4rem       (64px)
```

### Border Radius
```
--radius-sm    = 6px
--radius-md    = 10px
--radius-lg    = 16px
--radius-xl    = 24px
--radius-full  = 9999px (circles)
```

---

## 🌳 Component Examples

### Contest Card
```jsx
<div className="card card-hover">
  <div className="card-header">
    <h3 className="card-title">Contest Title</h3>
    <span className="badge statusActive">Live</span>
  </div>
  <p className="text-secondary">Description here...</p>
  <div className="card-footer">
    <span className="stat">5 problems</span>
    <span className="stat">42 participants</span>
  </div>
</div>
```

### Alert Component
```jsx
<div className="alert alert-info">
  <AlertCircle className="alert-icon" />
  <div className="alert-content">
    <div className="alert-title">Title</div>
    <div className="alert-description">Description text</div>
  </div>
  <button className="alert-close">×</button>
</div>
```

### Loading State
```jsx
<div className="loading-spinner">
  <div className="spinner"></div>
</div>

<div className="loading-text">
  <div className="spinner spinner-sm"></div>
  Loading...
</div>
```

### Empty State
```jsx
<div className="empty-state">
  <Trophy className="empty-state-icon" />
  <h3 className="empty-state-title">No Contests</h3>
  <p className="empty-state-description">Create your first contest to get started</p>
  <button className="btn btn-primary empty-state-action">
    Create Contest
  </button>
</div>
```

---

## 🎪 Card Hover Effects

All card components include premium hover effects:

1. **Border Color**: Changes from subtle `var(--border-subtle)` to cyan `rgba(0, 212, 255, 0.3)`
2. **Transform**: Lifts with `-6px` to `-8px` Y offset
3. **Shadow**: Expands with glowing cyan shadow
4. **Background**: Slightly increases opacity/brightness
5. **Gradient Overlay**: Subtle animated gradient passes through on hover

---

## 📱 Responsive Breakpoints

### Mobile Optimization
- **Tablets (768px and below)**:
  - Reduced font sizes on heading styles
  - Single-column layouts where applicable
  - Stacked button groups instead of horizontal
  - Adjusted spacing to conserve screen real estate

- **Mobile (480px and below)**:
  - Even smaller typography
  - Simplified grid layouts (1 column)
  - Increased touch target sizes (min 44x44px)
  - Reduced spacing

---

## 🚀 Best Practices

### Do's ✅
- Use CSS custom properties for all colors/spacing
- Apply animations with `--transition-base` (300ms) as default
- Use semantic HTML with proper ARIA labels
- Implement loading states for all async operations
- Include hover states for all interactive elements
- Test on multiple screen sizes and browsers

### Don'ts ❌
- Don't hardcode colors - use CSS variables
- Don't use animations shorter than 150ms or longer than 1s
- Don't apply same styling to different component types
- Don't forget focus states for accessibility
- Don't skip error states in forms

---

## 🎨 Common Color Combinations

### Success Flows
- Background: `var(--accent-green-dim)`
- Text: `var(--accent-green)`
- Border: `rgba(34, 197, 94, 0.3)`
- Shadow: `var(--shadow-glow-green)`

### Error Flows
- Background: `var(--accent-red-dim)`
- Text: `var(--accent-red)`
- Border: `rgba(239, 68, 68, 0.3)`
- Shadow: `var(--shadow-glow-red)`

### Info Flows
- Background: `var(--accent-cyan-dim)`
- Text: `var(--accent-cyan)`
- Border: `rgba(0, 212, 255, 0.3)`
- Shadow: `var(--shadow-glow-cyan)`

### Warning Flows
- Background: `var(--accent-amber-dim)`
- Text: `var(--accent-amber)`
- Border: `rgba(245, 158, 11, 0.3)`
- Shadow: `var(--shadow-glow-amber)`

---

## 📚 Resources

- **Design tokens**: `app/globals.css` (lines 1-103)
- **Typography**: JetBrains Mono (code), Inter (text)
- **Icons library**: Lucide React
- **Editor**: Monaco Editor with syntax highlighting

---

## 🚦 Version History

**Version 2.0** - Premium Polish Update
- Added glassmorphic enhancements
- Implemented comprehensive animation system
- Enhanced card hover effects with gradient overlays
- Added 50+ new utility classes
- Improved form element styling
- Added alert, modal, and empty state components
- Premium badge system with variants
- Responsive optimization for all screen sizes

---

## 📞 Questions?

Refer to specific component module CSS files for pixel-perfect implementation details:
- `app/page.module.css` - Home page styling
- `app/contests/contests.module.css` - Contests listing
- `app/contests/[id]/problems/[problemId]/problem.module.css` - Problem editor
- `app/components/Navbar.module.css` - Navigation bar
- `app/components/ContestCard.module.css` - Contest card component

