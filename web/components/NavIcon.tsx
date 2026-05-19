type Props = { name: string };

export default function NavIcon({ name }: Props) {
  const stroke = "currentColor";
  const sw = 1.6;
  switch (name) {
    case "leaf":
      return (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round">
          <path d="M5 19c0-8 6-14 14-14 0 8-6 14-14 14Z" /><path d="M5 19l8-8" />
        </svg>
      );
    case "cal":
      return (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke={stroke} strokeWidth={sw}>
          <rect x="4" y="5" width="16" height="15" rx="2" /><path d="M4 10h16M9 3v4M15 3v4" strokeLinecap="round" />
        </svg>
      );
    case "book":
      return (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round">
          <path d="M5 4h6a3 3 0 0 1 3 3v13" /><path d="M19 4h-6a3 3 0 0 0-3 3v13" /><path d="M5 4v15h14V4" />
        </svg>
      );
    case "swap":
      return (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 8h13l-3-3M20 16H7l3 3" />
        </svg>
      );
    case "knife":
      return (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 17l11-11 6 3-14 11z" /><path d="M3 17l3 3" />
        </svg>
      );
    case "compass":
      return (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke={stroke} strokeWidth={sw} strokeLinejoin="round">
          <circle cx="12" cy="12" r="9" /><path d="M15 9l-2 6-4 1 2-6z" fill="currentColor" stroke="none" opacity="0.7" />
        </svg>
      );
    case "flame":
      return (
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke={stroke} strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 2c0 4-4 5-4 9a4 4 0 0 0 8 0c0-4-4-5-4-9Z" />
          <path d="M12 12c0 2-1.5 3-1.5 4.5a1.5 1.5 0 0 0 3 0C13.5 15 12 14 12 12Z" fill="currentColor" stroke="none" opacity="0.5" />
        </svg>
      );
    default: return null;
  }
}
