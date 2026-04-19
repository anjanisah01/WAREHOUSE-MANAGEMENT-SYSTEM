import { useState } from 'react';

export default function HeroSection() {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <section className="relative min-h-screen overflow-hidden bg-[#0b0714] text-white">
      <video
        className="absolute inset-0 h-full w-full object-cover"
        autoPlay
        loop
        muted
        playsInline
      >
        <source
          src="https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260210_031346_d87182fb-b0af-4273-84d1-c6fd17d6bf0f.mp4"
          type="video/mp4"
        />
      </video>

      <div className="relative z-20 flex min-h-screen flex-col">
        <header className="relative z-20 flex w-full items-center justify-between gap-5 px-6 py-[16px] lg:px-[120px]">
          <div className="flex min-w-[150px] items-center gap-3">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M14 1.75L25.25 8.25V19.75L14 26.25L2.75 19.75V8.25L14 1.75ZM14 5.12L5.67 9.91V18.09L14 22.88L22.33 18.09V9.91L14 5.12ZM10.06 10.44H17.94V12.94H15.31V18.06H12.69V12.94H10.06V10.44Z" fill="white" />
            </svg>
            <span className="font-[Manrope] text-[15px] font-bold">Datacore</span>
          </div>

          <nav className="hidden flex-1 items-center justify-center gap-7 lg:flex">
            <a className="font-[Manrope] text-[14px] font-medium text-white transition hover:opacity-80" href="#home">Home</a>
            <a className="flex items-center gap-1 font-[Manrope] text-[14px] font-medium text-white transition hover:opacity-80" href="#services">
              Services
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M6 9L12 15L18 9" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </a>
            <a className="font-[Manrope] text-[14px] font-medium text-white transition hover:opacity-80" href="#reviews">Reviews</a>
            <a className="font-[Manrope] text-[14px] font-medium text-white transition hover:opacity-80" href="#contact">Contact us</a>
          </nav>

          <div className="hidden min-w-[220px] items-center justify-end gap-3 lg:flex">
            <button className="rounded-[8px] border border-[#d4d4d4] bg-white px-5 py-3 font-[Manrope] text-[14px] font-semibold text-[#171717]">Sign In</button>
            <button className="rounded-[8px] bg-[#7b39fc] px-5 py-3 font-[Manrope] text-[14px] font-semibold text-[#fafafa] shadow-[0_12px_24px_rgba(123,57,252,0.24)] transition hover:bg-[#8a50ff]">Get Started</button>
          </div>

          <button
            type="button"
            className="inline-flex h-11 w-11 items-center justify-center rounded-[10px] border border-white/20 bg-white/10 lg:hidden"
            onClick={() => setMenuOpen(true)}
            aria-label="Open menu"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M4 7H20M4 12H20M4 17H20" stroke="white" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        </header>

        {menuOpen ? (
          <div className="fixed inset-0 z-30 flex flex-col bg-black px-6 py-7 lg:hidden">
            <div className="mb-10 flex items-center justify-between">
              <span className="font-[Manrope] text-[15px] font-bold">Datacore</span>
              <button
                type="button"
                className="inline-flex h-11 w-11 items-center justify-center rounded-[10px] border border-white/20"
                onClick={() => setMenuOpen(false)}
                aria-label="Close menu"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M6 6L18 18M18 6L6 18" stroke="white" strokeWidth="2" strokeLinecap="round" />
                </svg>
              </button>
            </div>

            <nav className="flex flex-col gap-6">
              <a className="font-[Manrope] text-[28px] font-semibold text-white" href="#home" onClick={() => setMenuOpen(false)}>Home</a>
              <a className="font-[Manrope] text-[28px] font-semibold text-white" href="#services" onClick={() => setMenuOpen(false)}>Services</a>
              <a className="font-[Manrope] text-[28px] font-semibold text-white" href="#reviews" onClick={() => setMenuOpen(false)}>Reviews</a>
              <a className="font-[Manrope] text-[28px] font-semibold text-white" href="#contact" onClick={() => setMenuOpen(false)}>Contact us</a>
            </nav>

            <div className="mt-auto grid gap-3">
              <button className="rounded-[8px] border border-[#d4d4d4] bg-white px-5 py-3 font-[Manrope] text-[14px] font-semibold text-[#171717]">Sign In</button>
              <button className="rounded-[8px] bg-[#7b39fc] px-5 py-3 font-[Manrope] text-[14px] font-semibold text-[#fafafa] shadow-[0_12px_24px_rgba(123,57,252,0.24)]">Get Started</button>
            </div>
          </div>
        ) : null}

        <div className="relative z-20 flex flex-1 items-center justify-center px-6 pb-[72px] pt-[140px] lg:px-[120px]">
          <div className="mt-32 flex w-full max-w-[1040px] flex-col items-center text-center">
            <div className="mb-[22px] inline-flex min-h-[38px] items-center gap-[10px] rounded-[10px] border border-[rgba(164,132,215,0.5)] bg-[rgba(85,80,110,0.4)] px-[14px] py-[6px] backdrop-blur">
              <span className="rounded-[6px] bg-[#7b39fc] px-[10px] py-[4px] font-[Cabin] text-[14px] font-medium text-white">New</span>
              <span className="font-[Cabin] text-[14px] font-medium text-white">Say Hello to Datacore v3.2</span>
            </div>

            <h1 className="max-w-[1040px] font-[Instrument_Serif] text-5xl leading-[1.1] tracking-[-0.03em] text-white md:text-7xl lg:text-[96px]">
              Book your perfect stay instantly <span className="mx-[0.18em] italic">and</span> hassle-free
            </h1>

            <p className="mt-6 max-w-[662px] font-[Inter] text-[18px] font-normal leading-[1.75] text-white/70">
              Discover handpicked hotels, resorts, and stays across your favorite destinations. Enjoy exclusive deals, fast booking, and 24/7 support.
            </p>

            <div className="mt-[34px] flex w-full flex-col items-center justify-center gap-[14px] sm:w-auto sm:flex-row">
              <button className="w-full rounded-[10px] bg-[#7b39fc] px-6 py-4 font-[Cabin] text-[16px] font-medium text-white transition hover:bg-[#8a50ff] sm:w-auto">Book a Free Demo</button>
              <button className="w-full rounded-[10px] bg-[#2b2344] px-6 py-4 font-[Cabin] text-[16px] font-medium text-[#f6f7f9] transition hover:bg-[#3a2e5c] sm:w-auto">Get Started Now</button>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}