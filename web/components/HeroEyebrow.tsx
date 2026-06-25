import { UI } from "@/lib/strings";

export default function HeroEyebrow({ sub }: { sub: string }) {
  return (
    <div className="hero-eyebrow">
      <span className="hero-loc">{UI.hero.eyebrowLoc}</span>
      <span className="hero-dot">·</span>
      <span>{sub}</span>
    </div>
  );
}
