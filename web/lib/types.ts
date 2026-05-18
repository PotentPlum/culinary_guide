export type SavedRecipe = {
  id: string;
  name: string;
  ingredients: string[];
  instructions: string;
  source_url: string;
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
