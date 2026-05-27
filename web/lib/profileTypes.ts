export type Discipline = 'mechanical' | 'thermal' | 'flavor' | 'material';

export type UserAttempt = {
  id: string;
  user_id: string;
  source_type: 'challenge' | 'recipe';
  source_id: string | null;
  attempted_at: string;
  score: number | null;
  techniques: string[];
  cuisines: string[];
  ingredients: string[];
  taste_score: number | null;
  notes: string | null;
};

export type AttemptInput = Omit<UserAttempt, 'id' | 'user_id' | 'attempted_at'>;

export type DisciplineScore = {
  discipline: Discipline;
  label: string;
  frequency: number;
  proficiency: number;
};

export type ProfileStats = {
  disciplines: DisciplineScore[];
  topCuisines: Array<{ cuisine: string; count: number; avgScore: number }>;
  ingredientCloud: Array<{ ingredient: string; count: number }>;
  topTechniques: Array<{ techniqueId: string; count: number; avgScore: number }>;
  neglectedDisciplines: Discipline[];
  recentAttempts: UserAttempt[];
};

export type ArchetypeResult = {
  id: string;
  nl: string;
  description: string;
  earned: boolean;
};
