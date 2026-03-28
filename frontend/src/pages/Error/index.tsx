import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

const KONAMI = [
  "ArrowUp","ArrowUp","ArrowDown","ArrowDown",
  "ArrowLeft","ArrowRight","ArrowLeft","ArrowRight",
  "a","b",
];

const IMG = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTIHP2hZ02eKiTw47ujGcuVfYmZaUyhU2L1_Q&s";

const miniImages = [
  { style: { animation: "fly1 3.2s ease-in infinite", animationDelay: "0s" } },
  { style: { animation: "fly2 3.6s ease-in infinite", animationDelay: "0.6s" } },
  { style: { animation: "fly3 3s ease-in infinite", animationDelay: "1.2s" } },
  { style: { animation: "fly4 3.8s ease-in infinite", animationDelay: "0.3s" } },
  { style: { animation: "fly5 3.4s ease-in infinite", animationDelay: "1.8s" } },
];

const baseRow1 = [
  { h: "h-10", rotate: "-rotate-6", translateY: "translate-y-1"  },
  { h: "h-14", rotate: "rotate-3",  translateY: "-translate-y-1" },
  { h: "h-8",  rotate: "-rotate-2", translateY: "translate-y-2"  },
  { h: "h-16", rotate: "rotate-5",  translateY: "-translate-y-2" },
  { h: "h-11", rotate: "-rotate-4", translateY: "translate-y-1"  },
  { h: "h-13", rotate: "rotate-2",  translateY: "-translate-y-1" },
  { h: "h-9",  rotate: "-rotate-6", translateY: "translate-y-2"  },
];

const baseRow2 = [
  { h: "h-9",  rotate: "rotate-4",  translateY: "-translate-y-1" },
  { h: "h-12", rotate: "-rotate-5", translateY: "translate-y-2"  },
  { h: "h-7",  rotate: "rotate-2",  translateY: "translate-y-1"  },
  { h: "h-15", rotate: "-rotate-3", translateY: "-translate-y-2" },
  { h: "h-10", rotate: "rotate-6",  translateY: "translate-y-1"  },
  { h: "h-14", rotate: "-rotate-2", translateY: "-translate-y-1" },
  { h: "h-8",  rotate: "rotate-5",  translateY: "translate-y-2"  },
];

const ImageGroup = () => (
  <div className="flex items-end">
    <div className="relative w-48 h-48 rounded-full overflow-hidden shadow-lg shrink-0">
      <img src={IMG} alt="Foto 1" className="w-full h-full object-cover" />
      <div className="absolute inset-0 bg-[#c8a97e]/55" />
    </div>

    <div className="relative shrink-0">
      {miniImages.map((m, i) => (
        <div
          key={i}
          className="absolute top-0 left-1/2 -translate-x-1/2 w-12 h-16 rounded-md overflow-hidden shadow-md"
          style={m.style}
        >
          <img src={IMG} alt="" className="w-full h-full object-cover" />
          <div className="absolute inset-0 bg-white/60" />
        </div>
      ))}

      <div className="absolute bottom-14 left-0 w-72 flex items-end z-10">
        {baseRow2.map((s, i) => (
          <div key={i} className={`relative flex-1 ${s.h} overflow-hidden ${s.rotate} ${s.translateY}`}>
            <img src={IMG} alt="" className="w-full h-full object-cover" />
            <div className="absolute inset-0 bg-[#5c3d1e]/70" />
          </div>
        ))}
      </div>

      <div className="absolute bottom-0 left-0 w-72 flex items-end z-10">
        {baseRow1.map((s, i) => (
          <div key={i} className={`relative flex-1 ${s.h} overflow-hidden ${s.rotate} ${s.translateY}`}>
            <img src={IMG} alt="" className="w-full h-full object-cover" />
            <div className="absolute inset-0 bg-[#5c3d1e]/70" />
          </div>
        ))}
      </div>

      <div className="relative w-72 rounded-2xl overflow-hidden shadow-lg">
        <img src={IMG} alt="Foto 2" className="w-full" />
        <div
          className="absolute inset-0"
          style={{
            background:
              "linear-gradient(to top, rgba(200,169,126,0.6) 0%, rgba(200,169,126,0.6) 80%, rgba(220,38,38,0.6) 80%, rgba(220,38,38,0.6) 100%)",
          }}
        />
      </div>
    </div>

    <div className="relative w-48 h-48 rounded-full overflow-hidden shadow-lg shrink-0">
      <img src={IMG} alt="Foto 3" className="w-full h-full object-cover" />
      <div className="absolute inset-0 bg-[#c8a97e]/55" />
    </div>
  </div>
);

const scrollersRight = [
  { delay: "0s"   },
  { delay: "-4s"  },
  { delay: "-8s"  },
  { delay: "-12s" },
];

const scrollersLeft = [
  { delay: "0s"   },
  { delay: "-4s"  },
  { delay: "-8s"  },
  { delay: "-12s" },
];

export const ErrorPage = () => {
  const navigate = useNavigate();
  const [secretUnlocked, setSecretUnlocked] = useState(false);
  const inputRef = useRef<string[]>([]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      inputRef.current = [...inputRef.current, e.key].slice(-KONAMI.length);
      if (inputRef.current.join(",") === KONAMI.join(",")) {
        setSecretUnlocked(true);
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  if (!secretUnlocked) {
    return (
      <div className="relative flex flex-col items-center justify-center h-screen bg-[#eef0f7]">
        <button
          onClick={() => navigate("/")}
          className="absolute top-4 right-4 z-20 bg-[#0E1F63] text-white text-sm font-semibold px-4 py-2 rounded-xl shadow-lg hover:bg-[#162a85] transition-colors"
        >
          Voltar pra Home
        </button>
        <p className="text-8xl font-black text-[#0E1F63] mb-4">404</p>
        <p className="text-2xl font-semibold text-[#0E1F63] mb-2">Página não encontrada</p>
        <p className="text-sm text-gray-500">O endereço que você tentou acessar não existe.</p>
      </div>
    );
  }

  return (
    <div className="relative flex flex-col items-center justify-center h-screen bg-[#eef0f7] overflow-hidden">
      <button
        onClick={() => navigate("/")}
        className="absolute top-4 right-4 z-20 bg-[#0E1F63] text-white text-sm font-semibold px-4 py-2 rounded-xl shadow-lg hover:bg-[#162a85] transition-colors"
      >
        Voltar pra Home
      </button>
      <style>{`
        @keyframes fly1 {
          0%   { transform: translate(0, 0) rotate(0deg);            opacity: 1; }
          40%  { transform: translate(-65px, -240px) rotate(200deg);  opacity: 1; }
          80%  { transform: translate(-120px, 120px) rotate(480deg);  opacity: 1; }
          100% { transform: translate(-140px, 280px) rotate(560deg);  opacity: 0; }
        }
        @keyframes fly2 {
          0%   { transform: translate(0, 0) rotate(0deg);              opacity: 1; }
          40%  { transform: translate(-110px, -280px) rotate(-220deg); opacity: 1; }
          80%  { transform: translate(-200px, 130px) rotate(-520deg);  opacity: 1; }
          100% { transform: translate(-230px, 300px) rotate(-620deg);  opacity: 0; }
        }
        @keyframes fly3 {
          0%   { transform: translate(0, 0) rotate(0deg);          opacity: 1; }
          40%  { transform: translate(8px, -300px) rotate(300deg);  opacity: 1; }
          80%  { transform: translate(15px, 140px) rotate(650deg);  opacity: 1; }
          100% { transform: translate(18px, 310px) rotate(760deg);  opacity: 0; }
        }
        @keyframes fly4 {
          0%   { transform: translate(0, 0) rotate(0deg);             opacity: 1; }
          40%  { transform: translate(75px, -260px) rotate(-170deg);  opacity: 1; }
          80%  { transform: translate(140px, 115px) rotate(-430deg);  opacity: 1; }
          100% { transform: translate(160px, 280px) rotate(-510deg);  opacity: 0; }
        }
        @keyframes fly5 {
          0%   { transform: translate(0, 0) rotate(0deg);              opacity: 1; }
          40%  { transform: translate(120px, -270px) rotate(250deg);   opacity: 1; }
          80%  { transform: translate(220px, 125px) rotate(590deg);    opacity: 1; }
          100% { transform: translate(250px, 290px) rotate(700deg);    opacity: 0; }
        }

        @keyframes scrollRight {
          0%   { transform: translateX(-800px) rotate(0deg);   }
          100% { transform: translateX(100vw)  rotate(720deg); }
        }
        @keyframes bgPulse {
          0%,  100% { scale: 1;    }
          33%        { scale: 1.35; }
          66%        { scale: 0.7;  }
        }

        @keyframes scrollLeft {
          0%   { transform: translateX(calc(100vw + 200px)) rotate(0deg);    }
          100% { transform: translateX(-800px)               rotate(-720deg); }
        }
        @keyframes bgPulseSmall {
          0%,  100% { scale: 0.4;  }
          33%        { scale: 0.52; }
          66%        { scale: 0.3;  }
        }

        @keyframes gifLeft {
          0%    { transform: translateX(-110vw) translateY(0px);   opacity: 1; }
          100%  { transform: translateX(110vw)  translateY(0px);   opacity: 1; }
        }
        @keyframes gifRight {
          0%    { transform: translateX(110vw)  translateY(0px);   opacity: 1; }
          100%  { transform: translateX(-110vw) translateY(0px);   opacity: 1; }
        }
        @keyframes gifTop {
          0%    { transform: translateX(0px) translateY(-110vh);   opacity: 1; }
          100%  { transform: translateX(0px) translateY(110vh);    opacity: 1; }
        }
        @keyframes gifBottom {
          0%    { transform: translateX(0px) translateY(110vh);    opacity: 1; }
          100%  { transform: translateX(0px) translateY(-110vh);   opacity: 1; }
        }
        @keyframes gifDiagTLBR {
          0%    { transform: translateX(-110vw) translateY(-110vh); opacity: 1; }
          100%  { transform: translateX(110vw)  translateY(110vh);  opacity: 1; }
        }
        @keyframes gifDiagTRBL {
          0%    { transform: translateX(110vw)  translateY(-110vh); opacity: 1; }
          100%  { transform: translateX(-110vw) translateY(110vh);  opacity: 1; }
        }
        @keyframes gifDiagBLTR {
          0%    { transform: translateX(-110vw) translateY(110vh);  opacity: 1; }
          100%  { transform: translateX(110vw)  translateY(-110vh); opacity: 1; }
        }
      `}</style>

      {scrollersRight.map((g, i) => (
        <div key={i} className="absolute inset-0 flex items-center pointer-events-none" style={{ zIndex: 0 }}>
          <div
            style={{
              animation: "scrollRight 16s linear infinite, bgPulse 5s ease-in-out infinite",
              animationDelay: g.delay,
              opacity: 0.22,
            }}
          >
            <ImageGroup />
          </div>
        </div>
      ))}

      {scrollersLeft.map((g, i) => (
        <div key={i} className="absolute inset-0 flex items-center pointer-events-none" style={{ zIndex: 0 }}>
          <div
            style={{
              animation: "scrollLeft 14s linear infinite, bgPulseSmall 4s ease-in-out infinite",
              animationDelay: g.delay,
              opacity: 0.18,
            }}
          >
            <ImageGroup />
          </div>
        </div>
      ))}

      {[
        { anim: "gifLeft",    dur: "45s", delay: "0s"    },
        { anim: "gifRight",   dur: "40s", delay: "-10s"  },
        { anim: "gifTop",     dur: "50s", delay: "-22s"  },
        { anim: "gifBottom",  dur: "35s", delay: "-5s"   },
        { anim: "gifDiagTLBR",dur: "55s", delay: "-15s"  },
        { anim: "gifDiagTRBL",dur: "42s", delay: "-32s"  },
        { anim: "gifDiagBLTR",dur: "48s", delay: "-18s"  },
        { anim: "gifLeft",    dur: "60s", delay: "-40s"  },
        { anim: "gifRight",   dur: "38s", delay: "-28s"  },
        { anim: "gifTop",     dur: "52s", delay: "-8s"   },
        { anim: "gifBottom",  dur: "44s", delay: "-45s"  },
        { anim: "gifDiagTLBR",dur: "36s", delay: "-20s"  },
        { anim: "gifLeft",    dur: "43s", delay: "-33s"  },
        { anim: "gifRight",   dur: "57s", delay: "-12s"  },
        { anim: "gifTop",     dur: "39s", delay: "-50s"  },
        { anim: "gifBottom",  dur: "46s", delay: "-25s"  },
        { anim: "gifDiagTRBL",dur: "53s", delay: "-38s"  },
        { anim: "gifDiagBLTR",dur: "41s", delay: "-6s"   },
        { anim: "gifDiagTLBR",dur: "49s", delay: "-55s"  },
        { anim: "gifLeft",    dur: "37s", delay: "-17s"  },
        { anim: "gifRight",   dur: "61s", delay: "-43s"  },
        { anim: "gifTop",     dur: "34s", delay: "-30s"  },
        { anim: "gifBottom",  dur: "58s", delay: "-14s"  },
        { anim: "gifDiagTRBL",dur: "47s", delay: "-48s"  },
        { anim: "gifDiagBLTR",dur: "40s", delay: "-36s"  },
        { anim: "gifLeft",    dur: "54s", delay: "-21s"  },
        { anim: "gifRight",   dur: "44s", delay: "-58s"  },
        { anim: "gifDiagTLBR",dur: "62s", delay: "-26s"  },
        { anim: "gifTop",     dur: "33s", delay: "-42s"  },
        { anim: "gifBottom",  dur: "56s", delay: "-3s"   },
      ].map((g, i) => (
        <div key={i} className="absolute inset-0 flex items-center justify-center pointer-events-none" style={{ zIndex: 1 }}>
          <img
            src="https://media.tenor.com/Ui706-cdo7MAAAAM/whisky-combo-sabor-energetico.gif"
            alt=""
            className="h-36 rounded-xl shadow-lg"
            style={{
              animation: `${g.anim} ${g.dur} linear infinite`,
              animationDelay: g.delay,
            }}
          />
        </div>
      ))}

      <p className="relative z-10 text-2xl font-bold text-[#0E1F63] mb-6">SABOR ENERGÉTICO</p>

      <div className="relative z-10">
        <ImageGroup />
      </div>

      <p className="relative z-10 text-2xl font-bold text-[#0E1F63] mt-6">KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK TU É BETA</p>
    </div>
  );
};
