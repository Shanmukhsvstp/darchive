"use client";
import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";

/** Returns the signed-in user ({ id, email, fullName, role }) or null while loading. */
export function useMe() {
  const [me, setMe] = useState(null);
  useEffect(() => {
    apiFetch("/api/me").then(setMe).catch(() => {});
  }, []);
  return me;
}
