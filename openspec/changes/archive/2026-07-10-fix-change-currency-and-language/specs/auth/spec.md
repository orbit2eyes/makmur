# Domain: auth

## Purpose

The auth domain provides authentication and authorization for Makmur. Error responses follow a standardized JSON format with `error` (machine-readable code) and `message` (human-readable description) fields. The `message` field content is rendered in Indonesian for staff comprehension.

## MODIFIED Requirements

### Requirement: REQ-auth-014 - Standardized API error responses in Indonesian

- Description: All API endpoints SHALL return errors in a consistent JSON format with `error` (machine-readable snake_case code) and `message` (human-readable description in Indonesian) fields. The `error` code field SHALL remain in English (machine-readable). The `message` field SHALL use Indonesian translations. Error responses MUST never include stack traces, internal server paths, or SQL details.

  | HTTP Status | error_code | message (Indonesian) |
  |-------------|-----------|---------------------|
  | 400 | bad_request | `Permintaan tidak valid. Periksa kembali data yang dikirim.` |
  | 401 | unauthorized | `Sesi telah berakhir. Silakan login kembali.` |
  | 403 | forbidden | `Akses ditolak. Anda tidak memiliki izin yang cukup.` |
  | 403 | account_disabled | `Akun dinonaktifkan. Hubungi atasan Anda.` |
  | 404 | not_found | `Data tidak ditemukan.` |
  | 409 | conflict | `Data sudah ada. Gunakan nilai yang berbeda.` |
  | 422 | unprocessable_entity | `Data tidak valid. Periksa kembali input Anda.` |
  | 500 | internal_error | `Terjadi kesalahan. Coba lagi nanti.` |

- Priority: P0
- Rationale: Error messages are seen by staff when things go wrong. English messages cause confusion and support calls. Indonesian messages help staff understand and resolve issues independently.
- Change: Original error `message` fields were in English. New behavior uses Indonesian translations. The `error` code field remains English for machine processing.
- Acceptance criteria: AC3

#### Scenario: S-auth-025 - Error message in Indonesian

**Given** a staff member makes an API request that fails with 404
**When** the server returns the error response
**Then** the `message` field contains `Data tidak ditemukan.`
**And** the `error` field remains `not_found`

## Related Specifications

- **Product Spec** (`specs/product/spec.md`): Product controller error messages must also use Indonesian translations.
- **Scan Spec** (`specs/scan/spec.md`): Camera error handling messages must be translated to Indonesian.
