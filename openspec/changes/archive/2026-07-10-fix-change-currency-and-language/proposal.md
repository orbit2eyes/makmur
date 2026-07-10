---
slug: fix-change-currency-and-language
summary: Localize Makmur for Indonesian market — translate UI to Indonesian, change currency from USD to IDR
domains: [product]
status: planning
---

## Why

Target users are Indonesian retail staff. USD currency and English UI are confusing — staff think in Rupiah, price tags in stores use Rp. Error messages in English are hard to understand. The app will be misdirected if it keeps using USD and English.

## What Changes

1. Translate all user-facing UI text from English to Indonesian
2. Change price display format from USD ($) to IDR (Rp) with Indonesian number formatting
3. Translate server error response `message` fields to Indonesian
4. Keep database schema unchanged — `price` remains REAL, only display format changes

## Summary

A full Indonesian localization pass: UI strings, price formatting (Rp prefix, `.` as thousands separator), error messages. No schema changes, no locale switcher, no multi-language support — Indonesian-only in v1.
