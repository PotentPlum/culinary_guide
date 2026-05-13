import data from "@/data/vegetables.json";

export type Vegetable = {
  name: string;
  url: string;
  kenmerken: string;
  beschikbaarheid: string;
  voedingsstoffen?: string;
  in_de_keuken?: string;
  "hoe_bewaren?"?: string;
  start_month: number;
  end_month: number;
};

export const vegetables = data as Vegetable[];

export function isInSeason(v: Vegetable, month: number): boolean {
  if (v.start_month <= v.end_month) {
    return month >= v.start_month && month <= v.end_month;
  }
  // wraps around year-end (e.g. nov–feb)
  return month >= v.start_month || month <= v.end_month;
}

export function getInSeason(month: number): Vegetable[] {
  return vegetables.filter((v) => isInSeason(v, month));
}

export const MONTHS = [
  "Januari", "Februari", "Maart", "April", "Mei", "Juni",
  "Juli", "Augustus", "September", "Oktober", "November", "December",
];
