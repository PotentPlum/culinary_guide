export type SavedRecipe = {
  id: string;
  name: string;
  ingredients: string[];
  instructions: string;
  source_url: string;
  source: string;
  created_at: string;
  updated_at: string;
};

export type Challenge = {
  id: string;
  ingredient: string;
  challenge_text: string;
  difficulty?: "easy" | "medium" | "hard";
  created_at: string;
};

export type ChallengeParams = {
  technique?: string;
  cuisine?: string;
  timeLimit?: "<15" | "<30" | "<60" | ">60";
  simpleAndSmall?: boolean;
};

export type RecipeStep = {
  step: number;
  instruction: string;
};

export type RecipeIngredient = {
  amount: string;
  unit: string;
  name: string;
};

export type ChallengeResponse = {
  challenge: string;
  skill: string;
  difficulty: "Beginner" | "Intermediate" | "Advanced";
  timeEstimate: number;
  why: string;
  outputType: string;
  recipe: {
    title: string;
    servings: number;
    ingredients: RecipeIngredient[];
    steps: RecipeStep[];
    tips?: string[];
  };
};
