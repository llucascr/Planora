import type { SearchHeaderBtnType } from "../types/dataTable";

export const SearchHeaderBtn = ({ item }: { item: SearchHeaderBtnType[] }) => {
  return (
    <div className="flex gap-4 items-center max-lg:flex-wrap">
      {item.map((btn, index) => {
        if (btn.itemJsx) {
          return <div key={index}>{btn.itemJsx}</div>;
        } else {
          return (
            <button
              key={index}
              onClick={() => btn.onClick!()}
              className="cursor-pointer flex items-center gap-2 bg-transparent hover:bg-primary border hover:border-primary border-primary text-primary hover:text-white text-sm rounded-lg focus:ring-primary focus:border-primary p-2.5 w-auto"
            >
              {btn.title}
              {btn.icon}
            </button>
          );
        }
      })}
    </div>
  );
};
