"use client";

import { useState } from "react";
import { type VegItem } from "@/lib/vegetables";
import { UI } from "@/lib/strings";
import { supabase } from "@/lib/supabase";
import NavIcon from "@/components/NavIcon";
import SeasonalPage from "@/components/SeasonalPage";
import CalendarPage from "@/components/CalendarPage";
import DetailPanel from "@/components/DetailPanel";
import RecipesPage from "@/components/RecipesPage";
import TechniquesPage from "@/components/TechniquesPage";
import ChallengePage from "@/components/ChallengePage";
import ProfilePage from "@/components/ProfilePage";
import HomePage from "@/components/HomePage";

type Page = "home" | "seasonal" | "calendar" | "recipes" | "technieken" | "challenge" | "profiel";

const NAV_ITEMS = [
  { id: "home",          label: "Home",               icon: "home",    ready: true },
  { id: "seasonal",      label: UI.nav.seasonal,      icon: "leaf",    ready: true },
  { id: "calendar",      label: UI.nav.calendar,      icon: "cal",     ready: true },
  { id: "recipes",       label: UI.nav.recipes,       icon: "book",    ready: true },
  { id: "substitutions", label: UI.nav.substitutions, icon: "swap",    ready: false },
  { id: "technieken",    label: UI.nav.techniques,    icon: "knife",   ready: true },
  { id: "challenge",     label: UI.nav.challenge,     icon: "flame",   ready: true },
  { id: "profiel",       label: UI.nav.profile,       icon: "user",    ready: true },
  { id: "foraging",      label: UI.nav.foraging,      icon: "compass", ready: false },
];

export default function App() {
  const month = new Date().getMonth() + 1;
  const [page, setPage] = useState<Page>("home");
  const [openVeg, setOpenVeg] = useState<VegItem | null>(null);

  return (
    <div className="app">
      {/* Sidebar */}
      <aside className="sidenav">
        <div className="brand">
          <div className="brand-mark">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round">
              <path d="M5 19c0-8 6-14 14-14 0 8-6 14-14 14Z" />
              <path d="M5 19l8-8" />
            </svg>
          </div>
          <div className="brand-name">
            <div className="brand-title">{UI.brandTitle}</div>
            <div className="brand-sub">{UI.brandSub}</div>
          </div>
        </div>

        <nav className="nav">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              className={`nav-item${page === item.id ? " active" : ""}${!item.ready ? " soon" : ""}`}
              onClick={() => item.ready && setPage(item.id as Page)}
              disabled={!item.ready}
              title={item.ready ? item.label : `${item.label} — ${UI.nav.soon}`}
            >
              <NavIcon name={item.icon} />
              <span>{item.label}</span>
              {!item.ready && <span className="nav-soon">{UI.nav.soon}</span>}
            </button>
          ))}
        </nav>

        <div className="sidenav-footer">
          <div className="footer-loc">
            <span className="footer-flag" />
            <div>
              <div className="footer-loc-label">{UI.footer.regionLabel}</div>
              <div className="footer-loc-name">{UI.footer.regionName}</div>
            </div>
          </div>
          <div className="footer-build">{UI.footer.build}</div>
          <button
            className="logout-btn"
            onClick={async () => {
              await supabase.auth.signOut();
              window.location.href = "/login";
            }}
          >
            {UI.auth.logoutBtn}
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="main">
        {page === "home" && (
          <HomePage
            month={month}
            onNavigate={(p) => setPage(p)}
            onOpen={setOpenVeg}
          />
        )}

        {page === "seasonal" && <SeasonalPage month={month} onOpen={setOpenVeg} />}

        {page === "calendar" && (
          <CalendarPage month={month} onOpen={setOpenVeg} />
        )}

        {page === "recipes" && <RecipesPage />}

        {page === "technieken" && <TechniquesPage />}

        {page === "challenge" && <ChallengePage />}

        {page === "profiel" && <ProfilePage />}
      </main>

      <DetailPanel veg={openVeg} month={month} onClose={() => setOpenVeg(null)} />
    </div>
  );
}
