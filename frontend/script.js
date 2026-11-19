/* =========================================================
   FresherRadar – script.js

   Structure:
   1. CONFIG            – switch between sample data and the real API
   2. SAMPLE_JOBS       – demo records (replace with API data later)
   3. DATA ACCESS       – fetchJobs(): the ONLY function that knows where data comes from
   4. HELPERS           – formatting and safety utilities
   5. STATE             – what the UI is currently showing
   6. FILTER LOGIC      – search / filter / sort (pure functions)
   7. RENDERING         – turns data into HTML (cards, stats, companies)
   7b. DASHBOARD + SIDEBAR – insight panels, quick views, mobile menu, scroll-spy
   8. DRAWER            – job details panel
   9. EVENTS + STARTUP
   ========================================================= */

/* ---------------------------------------------------------
   1. CONFIG
   --------------------------------------------------------- */
const CONFIG = {
  // true  = use SAMPLE_JOBS below (Stage 1)
  // false = call the Spring Boot API at API_BASE_URL (later stages)
  USE_SAMPLE_DATA: false,
  API_BASE_URL: "https://fresherradar-backend.onrender.com/api",
  PAGE_SIZE: 12,              // how many cards to show per "Show more"
  SIMULATED_DELAY_MS: 800,    // fake network delay so the loading skeletons are visible
};

/* ---------------------------------------------------------
   2. SAMPLE DATA  (UI DEMONSTRATION ONLY – NOT REAL JOBS)

   Expected job object (this is the contract with the backend):
   {
     id:             number | string   unique id
     title:          string
     company:        string
     location:       string            city name, or "Remote (India)"
     category:       string            e.g. "Software Engineering"
     employmentType: "Full-time" | "Internship"
     experience:     "0 years" | "0-1 years" | "0-2 years"
     postedAt:       string            ISO-8601 date-time, e.g. "2026-11-14T09:30:00Z"
     source:         string            where the job was originally found
     applyUrl:       string            direct link to apply (http/https)
     description:    string
     skills:         string[]
     eligibility:    string[]
     isDemo:         boolean           optional – true for sample records
   }
   --------------------------------------------------------- */
const hoursAgo = (hours) => new Date(Date.now() - hours * 3600 * 1000).toISOString();
const daysAgo = (days) => hoursAgo(days * 24 + 3);

const ELIGIBILITY_TECH = [
  "B.E./B.Tech/MCA in CS, IT or a related field",
  "2025 or 2026 graduating batch",
  "No active backlogs",
];
const ELIGIBILITY_ANY_DEGREE = [
  "Any graduate or final-year student",
  "2025 or 2026 graduating batch",
  "Good written and spoken English",
];
const ELIGIBILITY_COMMERCE = [
  "B.Com, BBA, MBA (Finance) or CA Inter",
  "2025 or 2026 graduating batch",
  "Comfortable with spreadsheets",
];

const SAMPLE_JOBS = [
  {
    id: 1, title: "Backend Engineer Trainee (Java)", company: "Kiteline Systems", location: "Pune",
    category: "Software Engineering", employmentType: "Full-time", experience: "0 years",
    postedAt: hoursAgo(5), source: "Company Careers Page", applyUrl: "https://example.com/demo/jobs/1",
    description: "Join the platform team and learn to build REST services with Java and Spring Boot. You will pair with a senior engineer, ship small features in your first month and own a service within six months.",
    skills: ["Java", "Spring Boot", "SQL", "REST APIs", "Git"], eligibility: ELIGIBILITY_TECH, isDemo: true,
  },
  {
    id: 2, title: "Frontend Developer Intern", company: "Northwind Labs", location: "Bengaluru",
    category: "Software Engineering", employmentType: "Internship", experience: "0 years",
    postedAt: hoursAgo(3), source: "Company Careers Page", applyUrl: "https://example.com/demo/jobs/2",
    description: "A six-month internship building customer-facing dashboards. You will work on responsive layouts, accessibility and performance with an experienced mentor.",
    skills: ["HTML", "CSS", "JavaScript", "Responsive design"], eligibility: ELIGIBILITY_TECH, isDemo: true,
  },
  {
    id: 3, title: "Data Analyst Trainee", company: "Paperplane Analytics", location: "Hyderabad",
    category: "Data & Analytics", employmentType: "Full-time", experience: "0-1 years",
    postedAt: hoursAgo(20), source: "Campus Placement Portal", applyUrl: "https://example.com/demo/jobs/3",
    description: "Turn raw business data into dashboards and weekly insights for retail clients. Training in SQL and visualisation tools is provided in the first month.",
    skills: ["SQL", "Excel", "Power BI", "Statistics"], eligibility: ELIGIBILITY_ANY_DEGREE, isDemo: true,
  },
  {
    id: 4, title: "Machine Learning Intern", company: "Quillbyte AI", location: "Bengaluru",
    category: "AI / ML", employmentType: "Internship", experience: "0 years",
    postedAt: hoursAgo(30), source: "Community Job Board", applyUrl: "https://example.com/demo/jobs/4",
    description: "Help the research team prepare datasets, run model experiments and evaluate results for text classification products.",
    skills: ["Python", "scikit-learn", "Pandas", "Basic ML theory"], eligibility: ELIGIBILITY_TECH, isDemo: true,
  },
  {
    id: 5, title: "Associate Product Designer", company: "Marigold Studio", location: "Mumbai",
    category: "Design", employmentType: "Full-time", experience: "0-1 years",
    postedAt: daysAgo(2), source: "Company Careers Page", applyUrl: "https://example.com/demo/jobs/5",
    description: "Design end-to-end flows for a fintech mobile app. You will run quick user tests and hand off clean specs to engineers.",
    skills: ["Figma", "Wireframing", "Prototyping", "User research"],
    eligibility: ["Degree or diploma in design, HCI or a related field", "A portfolio with 2+ projects (college work is fine)", "2025 or 2026 graduating batch"], isDemo: true,
  },
  {
    id: 6, title: "Digital Marketing Intern", company: "Brightleaf Commerce", location: "Delhi NCR",
    category: "Marketing", employmentType: "Internship", experience: "0 years",
    postedAt: daysAgo(1), source: "Community Job Board", applyUrl: "https://example.com/demo/jobs/6",
    description: "Support social media, email and SEO campaigns for a D2C home goods brand. Learn how performance marketing works from the inside.",
    skills: ["Social media", "Copywriting", "Google Analytics", "Canva"], eligibility: ELIGIBILITY_ANY_DEGREE, isDemo: true,
  },
  {
    id: 7, title: "Business Development Associate", company: "Tessera Payments", location: "Mumbai",
    category: "Sales & BD", employmentType: "Full-time", experience: "0-1 years",
    postedAt: daysAgo(3), source: "Campus Placement Portal", applyUrl: "https://example.com/demo/jobs/7",
    description: "Reach out to small businesses, run product demos and help onboard new merchants. Strong communication matters more than prior experience.",
    skills: ["Communication", "Negotiation", "CRM basics", "Hindi and English"], eligibility: ELIGIBILITY_ANY_DEGREE, isDemo: true,
  },
  {
    id: 8, title: "Operations Analyst", company: "Cargolane Logistics", location: "Chennai",
    category: "Operations", employmentType: "Full-time", experience: "0-2 years",
    postedAt: daysAgo(4), source: "Company Careers Page", applyUrl: "https://example.com/demo/jobs/8",
    description: "Track shipments, spot delays and coordinate with warehouse teams to keep deliveries on time. Daily reporting in Excel.",
    skills: ["Excel", "Problem solving", "Process thinking"], eligibility: ELIGIBILITY_ANY_DEGREE, isDemo: true,
  },
  {
    id: 9, title: "Finance Intern", company: "Ledgerly", location: "Ahmedabad",
    category: "Finance", employmentType: "Internship", experience: "0 years",
    postedAt: daysAgo(4), source: "Campus Placement Portal", applyUrl: "https://example.com/demo/jobs/9",
    description: "Assist the accounts team with reconciliations, invoicing and month-end reporting for small business clients.",
    skills: ["Accounting basics", "Excel", "Tally"], eligibility: ELIGIBILITY_COMMERCE, isDemo: true,
  },
  {
    id: 10, title: "Customer Support Associate", company: "Bluepeak Software", location: "Remote (India)",
    category: "Customer Support", employmentType: "Full-time", experience: "0-1 years",
    postedAt: daysAgo(5), source: "Community Job Board", applyUrl: "https://example.com/demo/jobs/10",
    description: "Answer customer queries over chat and email for a SaaS product. Rotational shifts, full product training provided.",
    skills: ["Written English", "Empathy", "Typing speed", "Ticketing tools"], eligibility: ELIGIBILITY_ANY_DEGREE, isDemo: true,
  },
  {
    id: 11, title: "Talent Acquisition Intern", company: "Orchard People Co.", location: "Jaipur",
    category: "HR", employmentType: "Internship", experience: "0 years",
    postedAt: daysAgo(6), source: "Company Careers Page", applyUrl: "https://example.com/demo/jobs/11",
    description: "Source candidates, schedule interviews and help run campus hiring drives for client companies.",
    skills: ["Communication", "Sourcing", "Coordination", "MS Office"],
    eligibility: ["Pursuing or completed BBA, MBA (HR) or any graduation", "2025 or 2026 graduating batch", "Strong interpersonal skills"], isDemo: true,
  },
  {
    id: 12, title: "QA Engineer Trainee", company: "Stackwell Technologies", location: "Pune",
    category: "Software Engineering", employmentType: "Full-time", experience: "0-1 years",
    postedAt: daysAgo(6), source: "Campus Placement Portal", applyUrl: "https://example.com/demo/jobs/12",
    description: "Write test cases, run manual and automated tests and report bugs. A good first step into software engineering.",
    skills: ["Testing basics", "Selenium", "SQL", "Attention to detail"], eligibility: ELIGIBILITY_TECH, isDemo: true,
  },
  {
    id: 13, title: "Full Stack Developer Intern", company: "Zenfold Apps", location: "Remote (India)",
    category: "Software Engineering", employmentType: "Internship", experience: "0 years",
    postedAt: daysAgo(7), source: "Community Job Board", applyUrl: "https://example.com/demo/jobs/13",
    description: "Build features across a React front end and a Node.js back end for a habit-tracking app. Fully remote with weekly mentor check-ins.",
    skills: ["JavaScript", "Node.js", "React", "MongoDB"], eligibility: ELIGIBILITY_TECH, isDemo: true,
  },
  {
    id: 14, title: "Data Science Associate", company: "Cobalt Insights", location: "Bengaluru",
    category: "Data & Analytics", employmentType: "Full-time", experience: "0-2 years",
    postedAt: daysAgo(8), source: "Company Careers Page", applyUrl: "https://example.com/demo/jobs/14",
    description: "Build forecasting and segmentation models for consumer clients and present findings to non-technical stakeholders.",
    skills: ["Python", "SQL", "Statistics", "Data visualisation"],
    eligibility: ["B.E./B.Tech/M.Sc in a quantitative field", "Projects or internships in analytics are a plus", "2025 or 2026 graduating batch"], isDemo: true,
  },
  {
    id: 15, title: "Generative AI Engineer (Fresher)", company: "Quillbyte AI", location: "Hyderabad",
    category: "AI / ML", employmentType: "Full-time", experience: "0-1 years",
    postedAt: hoursAgo(9), source: "Company Careers Page", applyUrl: "https://example.com/demo/jobs/15",
    description: "Build LLM-powered features such as summarisation and classification. You will write prompts, evaluate outputs and connect models to production services.",
    skills: ["Python", "LLM APIs", "Prompt design", "REST APIs"], eligibility: ELIGIBILITY_TECH, isDemo: true,
  },
  {
    id: 16, title: "UI/UX Design Intern", company: "Marigold Studio", location: "Remote (India)",
    category: "Design", employmentType: "Internship", experience: "0 years",
    postedAt: daysAgo(10), source: "Community Job Board", applyUrl: "https://example.com/demo/jobs/16",
    description: "Support the design team with research synthesis, UI exploration and design-system upkeep.",
    skills: ["Figma", "Visual design", "Design systems"],
    eligibility: ["Student or recent graduate in design or a related field", "Portfolio link required", "2025 or 2026 graduating batch"], isDemo: true,
  },
  {
    id: 17, title: "Content Marketing Associate", company: "Brightleaf Commerce", location: "Delhi NCR",
    category: "Marketing", employmentType: "Full-time", experience: "0-1 years",
    postedAt: daysAgo(11), source: "Company Careers Page", applyUrl: "https://example.com/demo/jobs/17",
    description: "Write blog posts, product pages and email copy. You will learn to plan a content calendar and measure what works.",
    skills: ["Writing", "SEO basics", "Research", "Editing"], eligibility: ELIGIBILITY_ANY_DEGREE, isDemo: true,
  },
  {
    id: 18, title: "Sales Development Intern", company: "Tessera Payments", location: "Bengaluru",
    category: "Sales & BD", employmentType: "Internship", experience: "0 years",
    postedAt: daysAgo(12), source: "Campus Placement Portal", applyUrl: "https://example.com/demo/jobs/18",
    description: "Research prospects, write outreach emails and book meetings for the sales team.",
    skills: ["Research", "Email writing", "Communication"], eligibility: ELIGIBILITY_ANY_DEGREE, isDemo: true,
  },
  {
    id: 19, title: "DevOps Intern", company: "Riverstone Systems", location: "Kolkata",
    category: "Software Engineering", employmentType: "Internship", experience: "0 years",
    postedAt: daysAgo(14), source: "Community Job Board", applyUrl: "https://example.com/demo/jobs/19",
    description: "Learn CI/CD, containers and cloud basics while helping the team automate deployments.",
    skills: ["Linux", "Git", "Docker basics", "Shell scripting"], eligibility: ELIGIBILITY_TECH, isDemo: true,
  },
  {
    id: 20, title: "Financial Analyst Trainee", company: "Ledgerly", location: "Mumbai",
    category: "Finance", employmentType: "Full-time", experience: "0-1 years",
    postedAt: daysAgo(18), source: "Company Careers Page", applyUrl: "https://example.com/demo/jobs/20",
    description: "Build financial models, prepare budgets and support investor reporting for a growing accounting platform.",
    skills: ["Financial modelling", "Excel", "Accounting", "Presentation skills"], eligibility: ELIGIBILITY_COMMERCE, isDemo: true,
  },
  {
    id: 21, title: "Operations Intern", company: "Cargolane Logistics", location: "Chennai",
    category: "Operations", employmentType: "Internship", experience: "0 years",
    postedAt: daysAgo(21), source: "Campus Placement Portal", applyUrl: "https://example.com/demo/jobs/21",
    description: "Shadow the operations team, help with daily tracking sheets and learn how a last-mile logistics network runs.",
    skills: ["Excel", "Coordination", "Curiosity"], eligibility: ELIGIBILITY_ANY_DEGREE, isDemo: true,
  },
  {
    id: 22, title: "Android Developer Trainee", company: "Zenfold Apps", location: "Ahmedabad",
    category: "Software Engineering", employmentType: "Full-time", experience: "0-1 years",
    postedAt: daysAgo(25), source: "Company Careers Page", applyUrl: "https://example.com/demo/jobs/22",
    description: "Build and maintain features in a consumer Android app used by lakhs of people. Mentorship and code reviews from day one.",
    skills: ["Kotlin", "Android SDK", "REST APIs", "Git"], eligibility: ELIGIBILITY_TECH, isDemo: true,
  },
];

/* ---------------------------------------------------------
   3. DATA ACCESS
   Everything else in this file calls fetchJobs() and never
   cares where the data comes from.

   Expected API response (GET /api/jobs):
   { "lastUpdated": "2026-11-16T02:00:00Z", "jobs": [ ...job objects... ] }
   --------------------------------------------------------- */
async function fetchJobs() {
  // Add ?demoError=1 to the page URL to preview the error state.
  if (new URLSearchParams(window.location.search).has("demoError")) {
    await wait(CONFIG.SIMULATED_DELAY_MS);
    throw new Error("Demo error: the jobs service did not respond.");
  }

  if (CONFIG.USE_SAMPLE_DATA) {
    await wait(CONFIG.SIMULATED_DELAY_MS);
    return { lastUpdated: hoursAgo(2), jobs: SAMPLE_JOBS };
  }

  // Stage 2: the backend returns { lastUpdated, jobs }. size=1000 is the backend maximum,
  // which comfortably covers the 200+ listings the UI filters client-side.
  const response = await fetch(`${CONFIG.API_BASE_URL}/jobs?size=1000`);
  if (!response.ok) {
    throw new Error(`Server responded with status ${response.status}.`);
  }
  return response.json();
}

function wait(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

/* ---------------------------------------------------------
   4. HELPERS
   --------------------------------------------------------- */
// Always escape text before putting it in innerHTML (API data is untrusted).
function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

// Only allow http/https links, so a bad record cannot inject "javascript:" URLs.
function safeUrl(url) {
  try {
    const parsed = new URL(url);
    return parsed.protocol === "http:" || parsed.protocol === "https:" ? parsed.href : "#";
  } catch {
    return "#";
  }
}

function formatPostedDate(isoDate) {
  const hours = Math.floor((Date.now() - new Date(isoDate).getTime()) / 3600000);
  if (hours < 1) return "Just now";
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "Yesterday";
  if (days < 30) return `${days} days ago`;
  return new Date(isoDate).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

function formatFullDate(isoDate) {
  return new Date(isoDate).toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" });
}

function formatLastUpdated(isoDate) {
  const minutes = Math.max(1, Math.floor((Date.now() - new Date(isoDate).getTime()) / 60000));
  if (minutes < 60) return `Updated ${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `Updated ${hours}h ago`;
  return `Updated ${Math.floor(hours / 24)}d ago`;
}

function isPostedInLast24Hours(job) {
  return Date.now() - new Date(job.postedAt).getTime() < 24 * 3600 * 1000;
}

function isRemote(location) {
  return location.toLowerCase().startsWith("remote");
}

function uniqueSorted(values) {
  return [...new Set(values)].sort((a, b) => a.localeCompare(b));
}

// Gives each company a stable soft colour for its avatar.
function avatarStyle(companyName) {
  let hash = 0;
  for (const char of companyName) hash = (hash * 31 + char.charCodeAt(0)) % 360;
  return `background:hsl(${hash} 55% 93%);color:hsl(${hash} 45% 30%);`;
}

function companyInitial(companyName) {
  return escapeHtml(companyName.trim().charAt(0).toUpperCase());
}

/* ---------------------------------------------------------
   5. STATE
   --------------------------------------------------------- */
let allJobs = [];
let visibleCount = CONFIG.PAGE_SIZE;
let visibleCompanyRows = 4;
let lastFocusedElement = null;
let drawerJobIds = [];   // ids of the jobs the drawer can step through (Previous / Next)
let drawerIndex = 0;

const filters = {
  search: "",
  location: "",
  category: "",
  employmentType: "",
  experience: "",
  postedLast24h: false,   // set by the "New today" quick view
  sortBy: "newest",
};

/* ---------------------------------------------------------
   DOM references
   --------------------------------------------------------- */
const $ = (id) => document.getElementById(id);

const el = {
  header: $("siteHeader"), navToggle: $("navToggle"), navLinks: $("navLinks"),
  lastUpdatedText: $("lastUpdatedText"), demoBanner: $("demoBanner"),
  statTotal: $("statTotal"), statNew: $("statNew"), statCities: $("statCities"), statRoles: $("statRoles"),
  searchInput: $("searchInput"), filterLocation: $("filterLocation"), filterCategory: $("filterCategory"),
  filterType: $("filterType"), filterExperience: $("filterExperience"), sortBy: $("sortBy"),
  resultCount: $("resultCount"), clearFiltersBtn: $("clearFiltersBtn"),
  skeletonList: $("skeletonList"), jobList: $("jobList"),
  loadMoreWrap: $("loadMoreWrap"), loadMoreBtn: $("loadMoreBtn"),
  emptyState: $("emptyState"), emptyResetBtn: $("emptyResetBtn"),
  errorState: $("errorState"), errorMessage: $("errorMessage"), retryBtn: $("retryBtn"),
  companyGrid: $("companyGrid"), companyMoreWrap: $("companyMoreWrap"), companyMoreBtn: $("companyMoreBtn"),
  // sidebar + top bar
  sidebarOverlay: $("sidebarOverlay"), sidebarSync: $("sidebarSync"), quickViews: $("quickViews"),
  navJobsCount: $("navJobsCount"), navCompaniesCount: $("navCompaniesCount"),
  crumbCurrent: $("crumbCurrent"), topbarSearchBtn: $("topbarSearchBtn"),
  // overview
  overviewSubtitle: $("overviewSubtitle"),
  statTotalHint: $("statTotalHint"), statNewHint: $("statNewHint"), statCitiesHint: $("statCitiesHint"), statRolesHint: $("statRolesHint"),
  insights: $("insights"), insightRoles: $("insightRoles"), insightCities: $("insightCities"),
  insightMix: $("insightMix"), insightLatest: $("insightLatest"),
  // filters + drawer extras
  activeFilters: $("activeFilters"),
  drawerPrev: $("drawerPrev"), drawerNext: $("drawerNext"), drawerPosition: $("drawerPosition"), drawerFooter: $("drawerFooter"),
  drawer: $("jobDrawer"), drawerBody: $("drawerBody"), drawerClose: $("drawerClose"), drawerOverlay: $("drawerOverlay"),
};

/* ---------------------------------------------------------
   6. FILTER LOGIC
   --------------------------------------------------------- */
function getFilteredJobs() {
  const searchTerms = filters.search.trim().toLowerCase().split(/\s+/).filter(Boolean);

  const matches = allJobs.filter((job) => {
    if (filters.location && job.location !== filters.location) return false;
    if (filters.category && job.category !== filters.category) return false;
    if (filters.employmentType && job.employmentType !== filters.employmentType) return false;
    if (filters.experience && job.experience !== filters.experience) return false;
    if (filters.postedLast24h && !isPostedInLast24Hours(job)) return false;

    if (searchTerms.length > 0) {
      const searchableText = [job.title, job.company, job.category, job.location, ...(job.skills || [])]
        .join(" ").toLowerCase();
      // every word typed must appear somewhere in the job
      if (!searchTerms.every((term) => searchableText.includes(term))) return false;
    }
    return true;
  });

  matches.sort((a, b) => {
    const difference = new Date(b.postedAt) - new Date(a.postedAt);
    return filters.sortBy === "newest" ? difference : -difference;
  });
  return matches;
}

function hasActiveFilters() {
  return Boolean(filters.search.trim() || filters.location || filters.category || filters.employmentType || filters.experience || filters.postedLast24h);
}

function resetFilters() {
  filters.search = "";
  filters.location = "";
  filters.category = "";
  filters.employmentType = "";
  filters.experience = "";
  filters.postedLast24h = false;
  syncControlsFromFilters();
  refreshJobList();
}

// Copies the values in `filters` back into the search box and dropdowns.
// Needed whenever a filter is changed from somewhere other than the control itself
// (sidebar quick views, dashboard panels, filter chips).
function syncControlsFromFilters() {
  el.searchInput.value = filters.search;
  el.filterLocation.value = filters.location;
  el.filterCategory.value = filters.category;
  el.filterType.value = filters.employmentType;
  el.filterExperience.value = filters.experience;
}

function scrollToJobs() {
  document.getElementById("jobs").scrollIntoView({ behavior: "smooth" });
}

// Used by the dashboard panels: set one filter, then jump to the results.
function applyFilter(key, value) {
  filters[key] = value;
  syncControlsFromFilters();
  refreshJobList();
  scrollToJobs();
}

// Used by the removable chips under the search box.
function clearSingleFilter(key) {
  filters[key] = key === "postedLast24h" ? false : "";
  syncControlsFromFilters();
  refreshJobList();
}

function getRemoteLocationName() {
  const remoteJob = allJobs.find((job) => isRemote(job.location));
  return remoteJob ? remoteJob.location : "";
}

// Sidebar quick views work like toggles: click once to apply, click again to remove.
function toggleQuickView(key) {
  if (key === "all") {
    resetFilters();
    scrollToJobs();
    return;
  }
  if (key === "new") {
    filters.postedLast24h = !filters.postedLast24h;
  } else if (key === "remote") {
    filters.location = isRemote(filters.location) ? "" : getRemoteLocationName();
  } else {
    filters.employmentType = filters.employmentType === key ? "" : key;
  }
  syncControlsFromFilters();
  refreshJobList();
  scrollToJobs();
}

function updateQuickViewState() {
  el.quickViews.querySelectorAll("[data-quick]").forEach((button) => {
    const key = button.dataset.quick;
    let isActive;
    if (key === "all") isActive = !hasActiveFilters();
    else if (key === "new") isActive = filters.postedLast24h;
    else if (key === "remote") isActive = isRemote(filters.location);
    else isActive = filters.employmentType === key;
    button.classList.toggle("active", isActive);
    button.setAttribute("aria-pressed", String(isActive));
  });
}

// Removable chips ("Location: Pune ×") shown under the filters, plus highlighting
// of any dropdown that currently holds a value.
function renderActiveFilters() {
  const chips = [];
  if (filters.search.trim()) chips.push({ key: "search", name: "Search", label: `"${filters.search.trim()}"` });
  if (filters.location) chips.push({ key: "location", name: "Location", label: filters.location });
  if (filters.category) chips.push({ key: "category", name: "Role", label: filters.category });
  if (filters.employmentType) chips.push({ key: "employmentType", name: "Type", label: filters.employmentType });
  if (filters.experience) chips.push({ key: "experience", name: "Experience", label: filters.experience });
  if (filters.postedLast24h) chips.push({ key: "postedLast24h", name: "Posted", label: "Last 24 hours" });

  el.activeFilters.hidden = chips.length === 0;
  el.activeFilters.innerHTML = chips.map((chip) => `
    <button type="button" class="filter-chip" data-clear="${chip.key}"
            aria-label="Remove filter ${escapeHtml(chip.name)}: ${escapeHtml(chip.label)}">
      <span class="chip-key">${chip.name}</span>${escapeHtml(chip.label)}${ICON_CLOSE}
    </button>`).join("");

  el.filterLocation.closest(".field").classList.toggle("is-active", Boolean(filters.location));
  el.filterCategory.closest(".field").classList.toggle("is-active", Boolean(filters.category));
  el.filterType.closest(".field").classList.toggle("is-active", Boolean(filters.employmentType));
  el.filterExperience.closest(".field").classList.toggle("is-active", Boolean(filters.experience));
}

/* ---------------------------------------------------------
   7. RENDERING
   --------------------------------------------------------- */
const ICON_PIN = '<svg viewBox="0 0 16 16" aria-hidden="true"><path d="M8 14s5-4.2 5-8A5 5 0 0 0 3 6c0 3.8 5 8 5 8z" fill="none" stroke="currentColor" stroke-width="1.5"/><circle cx="8" cy="6" r="1.7" fill="currentColor"/></svg>';
const ICON_CLOCK = '<svg viewBox="0 0 16 16" aria-hidden="true"><circle cx="8" cy="8" r="6" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M8 5v3.2l2 1.3" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>';
const ICON_CLOSE = '<svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>';
const ICON_CHECK = '<svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>';
const ICON_LINK = '<svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/></svg>';

function typeChipClass(employmentType) {
  return employmentType === "Internship" ? "chip-type-internship" : "chip-type-fulltime";
}

function createJobCardHtml(job, index) {
  const isNew = isPostedInLast24Hours(job);
  // Stagger the entrance animation a little, but cap the delay.
  const animationDelay = Math.min(index, 8) * 30;
  const skills = job.skills || [];
  const skillTags = skills.slice(0, 3).map((skill) => `<span class="skill-tag">${escapeHtml(skill)}</span>`).join("");
  const moreSkills = skills.length > 3 ? `<span class="skill-tag skill-more">+${skills.length - 3}</span>` : "";

  return `
    <article class="job-card${isNew ? " is-new" : ""}${skills.length ? " has-skills" : " no-skills"}" data-id="${escapeHtml(job.id)}" style="animation-delay:${animationDelay}ms">
      <div class="job-card-top">
        <div class="avatar" style="${avatarStyle(job.company)}" aria-hidden="true">${companyInitial(job.company)}</div>
        <div class="job-heading">
          <button type="button" class="job-title" data-open="${escapeHtml(job.id)}">${escapeHtml(job.title)}</button>
          <p class="job-company">${escapeHtml(job.company)}</p>
        </div>
        ${isNew ? '<span class="badge-new">New</span>' : ""}
      </div>

      <ul class="job-meta">
        <li>${ICON_PIN}${escapeHtml(job.location)}</li>
        <li>${ICON_CLOCK}${escapeHtml(job.experience)} experience</li>
      </ul>

      <div class="chips">
        <span class="chip">${escapeHtml(job.category)}</span>
        <span class="chip ${typeChipClass(job.employmentType)}">${escapeHtml(job.employmentType)}</span>
      </div>
      ${skills.length ? `<div class="skill-row">${skillTags}${moreSkills}</div>` : ""}

      <div class="job-card-bottom">
        <div class="job-foot-meta">
          <span title="${escapeHtml(formatFullDate(job.postedAt))}">${escapeHtml(formatPostedDate(job.postedAt))}</span>
          · via <strong>${escapeHtml(job.source)}</strong>
        </div>
        <a class="btn btn-primary" href="${escapeHtml(safeUrl(job.applyUrl))}" target="_blank" rel="noopener noreferrer"
           aria-label="Apply for ${escapeHtml(job.title)} at ${escapeHtml(job.company)}">Apply</a>
      </div>
    </article>`;
}

function renderSkeletons(count = 6) {
  const card = `
    <div class="job-card skeleton-card">
      <div class="job-card-top">
        <span class="sk" style="width:40px;height:40px;border-radius:10px"></span>
        <div class="job-heading">
          <span class="sk" style="width:70%;height:16px"></span>
          <span class="sk" style="width:40%;height:12px;margin-top:8px"></span>
        </div>
      </div>
      <span class="sk" style="width:55%;height:12px"></span>
      <div class="chips">
        <span class="sk" style="width:100px;height:22px;border-radius:999px"></span>
        <span class="sk" style="width:72px;height:22px;border-radius:999px"></span>
      </div>
      <div class="job-card-bottom">
        <span class="sk" style="width:120px;height:12px"></span>
        <span class="sk" style="width:64px;height:32px"></span>
      </div>
    </div>`;
  el.skeletonList.innerHTML = card.repeat(count);
}

// Only one of: loading / error / ready is visible at a time.
function showView(view) {
  el.skeletonList.hidden = view !== "loading";
  el.errorState.hidden = view !== "error";
  const showResults = view === "ready";
  el.jobList.hidden = !showResults;
  if (!showResults) {
    el.emptyState.hidden = true;
    el.loadMoreWrap.hidden = true;
  }
}

function refreshJobList() {
  visibleCount = CONFIG.PAGE_SIZE;
  renderJobList();
}

function renderJobList() {
  const results = getFilteredJobs();
  const visibleJobs = results.slice(0, visibleCount);

  el.jobList.innerHTML = visibleJobs.map(createJobCardHtml).join("");

  const total = results.length;
  el.resultCount.textContent = total === 0
    ? "No jobs found"
    : `Showing ${visibleJobs.length} of ${total} ${total === 1 ? "job" : "jobs"}`;

  el.emptyState.hidden = total !== 0;
  el.jobList.hidden = total === 0;
  el.loadMoreWrap.hidden = visibleJobs.length >= total;
  el.clearFiltersBtn.hidden = !hasActiveFilters();

  renderActiveFilters();
  updateQuickViewState();
}

function renderStats() {
  const cities = new Set(allJobs.filter((job) => !isRemote(job.location)).map((job) => job.location));
  const roles = new Set(allJobs.map((job) => job.category));
  const newCount = allJobs.filter(isPostedInLast24Hours).length;
  const remoteCount = allJobs.filter((job) => isRemote(job.location)).length;
  const sourceCount = new Set(allJobs.map((job) => job.source)).size;
  const topRole = countBy(allJobs, (job) => job.category)[0];

  countUp(el.statTotal, allJobs.length);
  countUp(el.statNew, newCount);
  countUp(el.statCities, cities.size);
  countUp(el.statRoles, roles.size);

  el.statTotalHint.textContent = `From ${sourceCount} ${sourceCount === 1 ? "source" : "sources"}`;
  el.statNewHint.textContent = allJobs.length
    ? `${Math.round((newCount / allJobs.length) * 100)}% of all listings`
    : "No listings yet";
  el.statCitiesHint.textContent = remoteCount
    ? `Plus ${remoteCount} remote ${remoteCount === 1 ? "role" : "roles"}`
    : "No remote roles";
  el.statRolesHint.textContent = topRole ? `Top: ${topRole[0]}` : "Distinct categories";
}

function countUp(element, target) {
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (reduceMotion || target === 0) {
    element.textContent = target;
    return;
  }
  const duration = 600;
  const start = performance.now();
  function step(now) {
    const progress = Math.min((now - start) / duration, 1);
    element.textContent = Math.round(target * (1 - Math.pow(1 - progress, 3)));
    if (progress < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

function fillSelect(selectElement, values) {
  const options = values.map((value) => `<option value="${escapeHtml(value)}">${escapeHtml(value)}</option>`).join("");
  selectElement.insertAdjacentHTML("beforeend", options);
}

function renderFilterOptions() {
  // Reset to just the first "All ..." option, then add the real values.
  el.filterLocation.length = 1;
  el.filterCategory.length = 1;
  fillSelect(el.filterLocation, uniqueSorted(allJobs.map((job) => job.location)));
  fillSelect(el.filterCategory, uniqueSorted(allJobs.map((job) => job.category)));
}

function getCompanyColumns() {
  const template = window.getComputedStyle(el.companyGrid).gridTemplateColumns;
  return Math.max(1, template.split(/\s+/).filter(Boolean).length);
}

function renderCompanies() {
  // Group jobs by company name (like GROUP BY company in SQL).
  const companies = new Map();
  for (const job of allJobs) {
    if (!companies.has(job.company)) companies.set(job.company, { name: job.company, count: 0, cities: new Set() });
    const entry = companies.get(job.company);
    entry.count += 1;
    entry.cities.add(job.location);
  }

  const sorted = [...companies.values()].sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
  const pageSize = getCompanyColumns() * 4;
  el.companyGrid.dataset.totalCompanies = String(sorted.length);
  const visibleCompanies = sorted.slice(0, pageSize * visibleCompanyRows);

  el.companyGrid.innerHTML = visibleCompanies.map((company) => `
    <button type="button" class="company-card" data-company="${escapeHtml(company.name)}">
      <div class="company-top">
        <div class="avatar" style="${avatarStyle(company.name)}" aria-hidden="true">${companyInitial(company.name)}</div>
        <span class="company-count">${company.count} open ${company.count === 1 ? "role" : "roles"}</span>
      </div>
      <div>
        <div class="company-name">${escapeHtml(company.name)}</div>
        <div class="company-meta">${[...company.cities].map(escapeHtml).join(", ")}</div>
      </div>
    </button>`).join("");

  const hasMore = sorted.length > pageSize;
  el.companyMoreWrap.hidden = !hasMore;
  if (hasMore) {
    const showingAll = visibleCompanies.length >= sorted.length;
    el.companyMoreBtn.textContent = showingAll ? "Show less" : "See more companies";
    el.companyMoreBtn.dataset.showingAll = String(showingAll);
    el.companyMoreBtn.setAttribute("aria-expanded", String(visibleCompanyRows > 4));
  }
}

function renderLastUpdated(isoDate) {
  if (!isoDate) {   // backend has not completed a collection run yet
    el.lastUpdatedText.textContent = "No data yet";
    el.sidebarSync.textContent = "No data yet";
    return;
  }
  el.lastUpdatedText.textContent = formatLastUpdated(isoDate);
  el.sidebarSync.textContent = formatLastUpdated(isoDate);
  el.lastUpdatedText.parentElement.title = `Database last refreshed: ${new Date(isoDate).toLocaleString("en-IN")}`;
}

/* ---------------------------------------------------------
   7b. DASHBOARD PANELS + SIDEBAR
   --------------------------------------------------------- */
// Same idea as: SELECT key, COUNT(*) FROM jobs GROUP BY key ORDER BY COUNT(*) DESC
function countBy(jobs, getKey) {
  const counts = new Map();
  for (const job of jobs) {
    const key = getKey(job);
    counts.set(key, (counts.get(key) || 0) + 1);
  }
  return [...counts.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]));
}

function createBarListHtml(rows, filterKey) {
  if (rows.length === 0) return '<p class="panel-empty">No data yet.</p>';
  const maxCount = rows[0][1];
  return rows.map(([label, count], index) => `
    <button type="button" class="bar-row" data-filter-key="${filterKey}" data-filter-value="${escapeHtml(label)}"
            title="Show ${escapeHtml(label)} jobs">
      <span class="bar-label">${escapeHtml(label)}</span>
      <span class="bar-track"><span class="bar-fill" style="width:${Math.round((count / maxCount) * 100)}%;animation-delay:${index * 50}ms"></span></span>
      <span class="bar-value">${count}</span>
    </button>`).join("");
}

function renderInsightSkeletons() {
  const row = '<span class="sk" style="height:12px;margin:14px 0"></span>';
  el.insightRoles.innerHTML = row.repeat(5);
  el.insightCities.innerHTML = row.repeat(5);
  el.insightMix.innerHTML = '<span class="sk" style="height:10px"></span><span class="sk" style="height:52px;margin-top:12px"></span>';
  el.insightLatest.innerHTML = row.repeat(4);
}

function renderInsights() {
  const topRoles = countBy(allJobs, (job) => job.category)
    .filter(([label]) => !/^others?$/i.test(String(label).trim()))
    .slice(0, 8);
  el.insightRoles.innerHTML = createBarListHtml(topRoles, "category");
  el.insightCities.innerHTML = createBarListHtml(countBy(allJobs, (job) => job.location).slice(0, 8), "location");

  const fullTimeCount = allJobs.filter((job) => job.employmentType === "Full-time").length;
  const internshipCount = allJobs.filter((job) => job.employmentType === "Internship").length;
  const mixTotal = fullTimeCount + internshipCount;
  const percent = (count) => (mixTotal ? Math.round((count / mixTotal) * 100) : 0);

  el.insightMix.innerHTML = mixTotal === 0 ? '<p class="panel-empty">No data yet.</p>' : `
    <div class="mix-bar" role="img" aria-label="${fullTimeCount} full-time and ${internshipCount} internship jobs">
      <span class="mix-seg seg-fulltime" style="width:${percent(fullTimeCount)}%"></span>
      <span class="mix-seg seg-internship" style="width:${percent(internshipCount)}%"></span>
    </div>
    <div class="mix-legend">
      <button type="button" class="mix-item" data-filter-key="employmentType" data-filter-value="Full-time">
        <span class="mix-name"><i class="mix-dot seg-fulltime"></i>Full-time</span>
        <span class="mix-value">${fullTimeCount}<small>${percent(fullTimeCount)}%</small></span>
      </button>
      <button type="button" class="mix-item" data-filter-key="employmentType" data-filter-value="Internship">
        <span class="mix-name"><i class="mix-dot seg-internship"></i>Internships</span>
        <span class="mix-value">${internshipCount}<small>${percent(internshipCount)}%</small></span>
      </button>
    </div>`;

  const latestJobs = [...allJobs].sort((a, b) => new Date(b.postedAt) - new Date(a.postedAt)).slice(0, 3);
  el.insightLatest.innerHTML = latestJobs.map((job) => `
    <button type="button" class="latest-item" data-open="${escapeHtml(job.id)}">
      <span class="avatar" style="${avatarStyle(job.company)}" aria-hidden="true">${companyInitial(job.company)}</span>
      <span>
        <span class="latest-title">${escapeHtml(job.title)}</span>
        <span class="latest-meta">${escapeHtml(job.company)} · ${escapeHtml(job.location)}</span>
      </span>
      <span class="latest-time">${escapeHtml(formatPostedDate(job.postedAt))}</span>
    </button>`).join("") || '<p class="panel-empty">No jobs yet.</p>';
}

// Counts shown next to the sidebar links.
function renderQuickViews() {
  const counts = {
    all: allJobs.length,
    new: allJobs.filter(isPostedInLast24Hours).length,
    Internship: allJobs.filter((job) => job.employmentType === "Internship").length,
    "Full-time": allJobs.filter((job) => job.employmentType === "Full-time").length,
    remote: allJobs.filter((job) => isRemote(job.location)).length,
  };
  document.querySelectorAll("[data-quick-count]").forEach((badge) => {
    badge.textContent = counts[badge.dataset.quickCount];
  });
  el.quickViews.querySelector('[data-quick="remote"]').disabled = counts.remote === 0;
  el.navJobsCount.textContent = allJobs.length;
  el.navCompaniesCount.textContent = new Set(allJobs.map((job) => job.company)).size;
}

// Mobile / tablet: the sidebar slides in over the page.
function setSidebarOpen(isOpen) {
  document.body.classList.toggle("nav-open", isOpen);
  el.navToggle.setAttribute("aria-expanded", String(isOpen));
  el.navToggle.setAttribute("aria-label", isOpen ? "Close menu" : "Open menu");
}

// Highlights the sidebar link of the section currently on screen.
const SECTION_TITLES = { overview: "Overview", jobs: "Jobs", companies: "Companies", about: "About" };

function setActiveSection(sectionId) {
  el.navLinks.querySelectorAll("[data-section]").forEach((link) => {
    const isCurrent = link.dataset.section === sectionId;
    link.classList.toggle("active", isCurrent);
    if (isCurrent) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
  el.crumbCurrent.textContent = SECTION_TITLES[sectionId] || "Overview";
}

function setupScrollSpy() {
  setActiveSection("overview");
  if (!("IntersectionObserver" in window)) return;
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) setActiveSection(entry.target.id);
    });
  }, { rootMargin: "-20% 0px -70% 0px" });
  Object.keys(SECTION_TITLES).forEach((id) => observer.observe(document.getElementById(id)));
}

/* ---------------------------------------------------------
   8. JOB DETAILS DRAWER
   --------------------------------------------------------- */
function openJobDrawer(jobId) {
  const job = allJobs.find((item) => String(item.id) === String(jobId));
  if (!job) return;

  const wasOpen = isDrawerOpen();
  if (!wasOpen) {
    // Remember the current result order so Previous / Next walk through the same list.
    drawerJobIds = getFilteredJobs().map((item) => String(item.id));
    lastFocusedElement = document.activeElement;
  }
  if (!drawerJobIds.includes(String(job.id))) drawerJobIds = [String(job.id)];
  drawerIndex = drawerJobIds.indexOf(String(job.id));

  const applyLink = safeUrl(job.applyUrl);
  const skillChips = (job.skills || []).map((skill) => `<span class="chip chip-skill">${escapeHtml(skill)}</span>`).join("");
  const eligibilityItems = (job.eligibility || []).map((item) => `<li>${ICON_CHECK}<span>${escapeHtml(item)}</span></li>`).join("");

  el.drawerBody.innerHTML = `
    <div class="detail-top">
      <div class="avatar" style="${avatarStyle(job.company)}" aria-hidden="true">${companyInitial(job.company)}</div>
      <div>
        <h2 class="detail-title" id="drawerTitle">${escapeHtml(job.title)}</h2>
        <p class="detail-company">${escapeHtml(job.company)} · Posted ${escapeHtml(formatPostedDate(job.postedAt))}</p>
      </div>
      ${isPostedInLast24Hours(job) ? '<span class="badge-new">New</span>' : ""}
    </div>

    <dl class="facts">
      <div class="fact"><dt>Location</dt><dd>${escapeHtml(job.location)}</dd></div>
      <div class="fact"><dt>Role</dt><dd>${escapeHtml(job.category)}</dd></div>
      <div class="fact"><dt>Employment type</dt><dd>${escapeHtml(job.employmentType)}</dd></div>
      <div class="fact"><dt>Experience</dt><dd>${escapeHtml(job.experience)}<small>Posted ${escapeHtml(formatFullDate(job.postedAt))}</small></dd></div>
    </dl>

    <section class="detail-section">
      <h3>About the role</h3>
      <p>${escapeHtml(job.description)}</p>
    </section>

    <section class="detail-section">
      <h3>Required skills</h3>
      <div class="chips">${skillChips || "<p>Not specified.</p>"}</div>
    </section>

    <section class="detail-section">
      <h3>Eligibility</h3>
      ${eligibilityItems ? `<ul class="check-list">${eligibilityItems}</ul>` : "<p>Not specified.</p>"}
    </section>

    <section class="detail-section">
      <h3>Original source</h3>
      <div class="source-card">
        <span class="source-icon">${ICON_LINK}</span>
        <div class="source-info">
          <p class="source-name">${escapeHtml(job.source)}</p>
          <p class="source-note">Always verify details on the employer's own page before applying.</p>
        </div>
        <a class="link-button" href="${escapeHtml(applyLink)}" target="_blank" rel="noopener noreferrer">Open listing ↗</a>
      </div>
    </section>
    ${job.isDemo ? '<p class="detail-demo"><strong>Demo record.</strong> This is sample data for UI demonstration and not a real opening.</p>' : ""}`;

  el.drawerFooter.innerHTML = `
    <a class="btn btn-primary btn-lg btn-block" href="${escapeHtml(applyLink)}" target="_blank" rel="noopener noreferrer">Apply on original site ↗</a>
    <p class="note">Opens ${escapeHtml(job.source)} in a new tab</p>`;

  el.drawerPosition.textContent = drawerJobIds.length > 1 ? `${drawerIndex + 1} of ${drawerJobIds.length}` : "";
  el.drawerPrev.disabled = drawerIndex <= 0;
  el.drawerNext.disabled = drawerIndex >= drawerJobIds.length - 1;

  document.body.classList.add("drawer-open", "no-scroll");
  el.drawer.setAttribute("aria-hidden", "false");
  el.drawerBody.scrollTop = 0;
  if (!wasOpen || document.activeElement.disabled) el.drawerClose.focus();
}

// Previous / Next job inside the drawer (direction is -1 or +1).
function stepDrawer(direction) {
  const nextIndex = drawerIndex + direction;
  if (nextIndex < 0 || nextIndex >= drawerJobIds.length) return;
  openJobDrawer(drawerJobIds[nextIndex]);
}

function closeJobDrawer() {
  document.body.classList.remove("drawer-open", "no-scroll");
  el.drawer.setAttribute("aria-hidden", "true");
  if (lastFocusedElement) lastFocusedElement.focus();
}

function isDrawerOpen() {
  return document.body.classList.contains("drawer-open");
}

// Keeps Tab inside the drawer while it is open.
function trapFocusInDrawer(event) {
  const focusable = el.drawer.querySelectorAll("a[href], button:not([disabled])");
  if (focusable.length === 0) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

/* ---------------------------------------------------------
   9. EVENTS + STARTUP
   --------------------------------------------------------- */
function debounce(callback, delayMs) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => callback(...args), delayMs);
  };
}

function bindEvents() {
  // Search (debounced so we do not re-render on every keystroke)
  el.searchInput.addEventListener("input", debounce(() => {
    filters.search = el.searchInput.value;
    refreshJobList();
  }, 180));

  // Dropdown filters
  const bindSelect = (selectElement, filterKey) => {
    selectElement.addEventListener("change", () => {
      filters[filterKey] = selectElement.value;
      refreshJobList();
    });
  };
  bindSelect(el.filterLocation, "location");
  bindSelect(el.filterCategory, "category");
  bindSelect(el.filterType, "employmentType");
  bindSelect(el.filterExperience, "experience");
  bindSelect(el.sortBy, "sortBy");

  el.clearFiltersBtn.addEventListener("click", resetFilters);
  el.emptyResetBtn.addEventListener("click", resetFilters);
  el.loadMoreBtn.addEventListener("click", () => {
    visibleCount += CONFIG.PAGE_SIZE;
    renderJobList();
  });
  el.retryBtn.addEventListener("click", loadJobs);

  // One click listener for the whole list (event delegation):
  // clicking anywhere on a card opens details, except on the Apply link.
  el.jobList.addEventListener("click", (event) => {
    if (event.target.closest("a")) return;
    const card = event.target.closest(".job-card");
    if (card) openJobDrawer(card.dataset.id);
  });

  // Clicking a company filters the job list by that company.
  el.companyGrid.addEventListener("click", (event) => {
    const card = event.target.closest(".company-card");
    if (!card) return;
    resetFilters();
    filters.search = card.dataset.company;
    el.searchInput.value = card.dataset.company;
    refreshJobList();
    scrollToJobs();
  });

  el.companyMoreBtn.addEventListener("click", () => {
    const pageSize = getCompanyColumns() * 4;
    const currentlyShowingAll = el.companyMoreBtn.dataset.showingAll === "true";
    visibleCompanyRows = currentlyShowingAll ? 4 : visibleCompanyRows + 4;
    renderCompanies();
    if (currentlyShowingAll) {
      document.getElementById("companies").scrollIntoView({ behavior: "smooth", block: "start" });
    }
  });

  window.addEventListener("resize", debounce(() => renderCompanies(), 160));

  // Dashboard panels: bars and legend filter the list, "latest" rows open details.
  el.insights.addEventListener("click", (event) => {
    const filterButton = event.target.closest("[data-filter-key]");
    if (filterButton) {
      applyFilter(filterButton.dataset.filterKey, filterButton.dataset.filterValue);
      return;
    }
    const openButton = event.target.closest("[data-open]");
    if (openButton) openJobDrawer(openButton.dataset.open);
  });

  // Removable filter chips under the search box
  el.activeFilters.addEventListener("click", (event) => {
    const chip = event.target.closest("[data-clear]");
    if (chip) clearSingleFilter(chip.dataset.clear);
  });

  // Sidebar quick views
  el.quickViews.addEventListener("click", (event) => {
    const button = event.target.closest("[data-quick]");
    if (!button || button.disabled) return;
    toggleQuickView(button.dataset.quick);
    setSidebarOpen(false);
  });

  // Top bar search shortcut: jump to the jobs section and focus the search box
  el.topbarSearchBtn.addEventListener("click", () => {
    scrollToJobs();
    el.searchInput.focus({ preventScroll: true });
  });

  // Drawer
  el.drawerClose.addEventListener("click", closeJobDrawer);
  el.drawerOverlay.addEventListener("click", closeJobDrawer);
  el.drawerPrev.addEventListener("click", () => stepDrawer(-1));
  el.drawerNext.addEventListener("click", () => stepDrawer(1));

  // Mobile navigation (sidebar slides in)
  el.navToggle.addEventListener("click", () => {
    setSidebarOpen(!document.body.classList.contains("nav-open"));
  });
  el.sidebarOverlay.addEventListener("click", () => setSidebarOpen(false));
  el.navLinks.addEventListener("click", (event) => {
    if (event.target.closest("a")) setSidebarOpen(false);
  });

  // Keyboard: Esc closes drawer/menu, "/" focuses search, arrows step through jobs,
  // Tab is trapped in the drawer
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      if (isDrawerOpen()) closeJobDrawer();
      setSidebarOpen(false);
    }
    if (event.key === "Tab" && isDrawerOpen()) trapFocusInDrawer(event);

    const typingInField = ["INPUT", "SELECT", "TEXTAREA"].includes(document.activeElement.tagName);
    if (event.key === "/" && !typingInField && !isDrawerOpen()) {
      event.preventDefault();
      el.searchInput.focus();
    }
    if (isDrawerOpen() && !typingInField) {
      if (event.key === "ArrowLeft") stepDrawer(-1);
      if (event.key === "ArrowRight") stepDrawer(1);
    }
  });
}

async function loadJobs() {
  showView("loading");
  renderInsightSkeletons();
  el.resultCount.textContent = "Loading jobs…";
  el.clearFiltersBtn.hidden = true;

  try {
    const data = await fetchJobs();
    allJobs = data.jobs || [];

    renderLastUpdated(data.lastUpdated);
    renderStats();
    renderInsights();
    renderQuickViews();
    renderFilterOptions();
    renderCompanies();
    el.demoBanner.hidden = !allJobs.some((job) => job.isDemo);

    showView("ready");
    refreshJobList();
  } catch (error) {
    console.error("Failed to load jobs:", error);
    el.errorMessage.textContent = error.message || "Something went wrong while fetching job listings.";
    el.resultCount.textContent = "";
    el.lastUpdatedText.textContent = "Update unavailable";
    el.sidebarSync.textContent = "Unavailable";
    showView("error");
  }
}

function init() {
  $("year").textContent = new Date().getFullYear();
  el.overviewSubtitle.textContent = new Date().toLocaleDateString("en-IN", {
    weekday: "long", day: "numeric", month: "long",
  }) + " · Fresher jobs and internships across India";
  renderSkeletons();
  bindEvents();
  setupScrollSpy();
  loadJobs();
}

init();
