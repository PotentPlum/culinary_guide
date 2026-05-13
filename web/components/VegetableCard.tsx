import type { Vegetable } from "@/lib/vegetables";

type Props = {
  vegetable: Vegetable;
  onClick: () => void;
};

export default function VegetableCard({ vegetable, onClick }: Props) {
  return (
    <button
      onClick={onClick}
      className="bg-white rounded-xl border border-stone-200 p-3 text-left shadow-sm hover:shadow-md hover:border-green-400 transition-all active:scale-95"
    >
      <p className="font-semibold text-stone-800 text-sm leading-tight">{vegetable.name}</p>
      <p className="text-stone-400 text-xs mt-1 line-clamp-2">{vegetable.kenmerken}</p>
    </button>
  );
}
