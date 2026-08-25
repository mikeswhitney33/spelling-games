"use client";

import { useEffect, useState } from "react";

/** Whether the browser can speak words aloud via speech synthesis. */
export function useSpeechSupported(): boolean {
  const [supported, setSupported] = useState(true);
  useEffect(() => {
    setSupported(typeof window !== "undefined" && "speechSynthesis" in window);
  }, []);
  return supported;
}
