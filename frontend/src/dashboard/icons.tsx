import type { ReactNode } from "react";

const paths = {
  inbox: (
    <>
      <path d="M4 4h16v16H4zM4 14h5l2 3h2l2-3h5" />
    </>
  ),
  messages: (
    <>
      <path d="M20 11a7 7 0 0 1-7 7H7l-4 3V10a7 7 0 0 1 7-7h3a7 7 0 0 1 7 8Z" />
      <path d="M8 8h7M8 12h5" />
    </>
  ),
  history: (
    <>
      <path d="M3 11a9 9 0 1 1 3 8M3 4v7h7M12 7v5l3 2" />
    </>
  ),
  settings: (
    <>
      <path d="m10 3-1 3-3 1-3 3 2 3v3l3 2 2 3h4l1-3 3-1 3-3-2-3V8l-3-2-2-3Z" />
      <circle cx="12" cy="12" r="3" />
    </>
  ),
  sparkles: (
    <>
      <path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5ZM20 2v4M18 4h4" />
    </>
  ),
  clock: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  users: (
    <>
      <circle cx="9" cy="8" r="3" />
      <path d="M3 21v-3a6 6 0 0 1 12 0v3M16 5a3 3 0 0 1 0 6M17 15a5 5 0 0 1 4 5" />
    </>
  ),
  search: (
    <>
      <circle cx="10" cy="10" r="6" />
      <path d="m15 15 5 5" />
    </>
  ),
  bell: (
    <>
      <path d="M5 16h14l-2-4V9a5 5 0 0 0-10 0v3ZM10 20h4" />
    </>
  ),
  refresh: (
    <>
      <path d="M20 10a8 8 0 0 0-14-5L3 8M3 3v5h5M4 14a8 8 0 0 0 14 5l3-3M16 16h5v5" />
    </>
  ),
  sliders: (
    <>
      <path d="M4 7h6M14 7h6M4 17h10M18 17h2M10 4v6M14 14v6" />
    </>
  ),
  "chevron-right": <path d="m9 6 6 6-6 6" />,
  "chevron-left": <path d="m15 6-6 6 6 6" />,
  close: <path d="m6 6 12 12M6 18 18 6" />,
  whatsapp: (
    <>
      <path d="M20 11.5A8.5 8.5 0 0 1 7 19l-5 2 2-5A8.5 8.5 0 1 1 20 11.5Z" />
      <path d="M8 7c-2 4 3 9 7 7l-2-2-1 1-3-3 1-1Z" />
    </>
  ),
  send: (
    <>
      <path d="m3 3 18 9-18 9 3-9ZM6 12h15" />
    </>
  ),
  check: <path d="m5 12 4 4L19 6" />,
  transfer: (
    <>
      <path d="M4 8h15l-4-4M20 16H5l4 4" />
    </>
  ),
  monitor: (
    <>
      <rect x="3" y="4" width="18" height="13" rx="2" />
      <path d="M8 21h8M12 17v4" />
    </>
  ),
} satisfies Record<string, ReactNode>;

export type IconName = keyof typeof paths;
export function Icon({ name }: { name: IconName }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      {paths[name]}
    </svg>
  );
}
