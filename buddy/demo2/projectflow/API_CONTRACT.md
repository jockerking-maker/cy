# ProjectFlow API Contract

## Base URL
`/api/v1`

## Response Format
All responses follow a unified structure:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

## Status Codes
| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (missing/invalid token) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Not Found |
| 500 | Internal Server Error |

## Pagination Format
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [...],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "total": 100,
      "totalPages": 5
    }
  }
}
```

## Authentication
All protected endpoints require the `Authorization: Bearer <token>` header.
JWT token contains: `{ id, username, role_id, role_name }`

## Endpoints

### Auth (`/api/v1/auth`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/register` | No | Register new user |
| POST | `/login` | No | Login, returns JWT token |
| GET | `/profile` | Yes | Get current user profile |
| PUT | `/profile` | Yes | Update profile (display_name, email, phone, avatar) |
| PUT | `/password` | Yes | Change password (old_password, new_password) |

### Users (`/api/v1/users`) — Admin only
| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | List users (paginated, search, sort) |
| GET | `/:id` | Get user by ID |
| POST | `/` | Create user |
| PUT | `/:id` | Update user |
| DELETE | `/:id` | Delete user (cannot delete self) |
| PUT | `/:id/role` | Change user role (role_id) |

### Items (`/api/v1/items`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | List items (paginated, search, filter, sort) |
| GET | `/:id` | Get item by ID |
| POST | `/` | Create item |
| PUT | `/:id` | Update item |
| DELETE | `/:id` | Delete item |
| POST | `/batch-delete` | Batch delete (ids array) |

**Query parameters for list:**
- `page`, `pageSize` - pagination
- `search` - search title/description
- `category`, `status`, `priority` - filters
- `sort` - sort field (created_at, updated_at, title, priority, due_date, status, category)
- `order` - sort order (asc/desc)

### Articles (`/api/v1/articles`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | List articles (paginated, search, filter) |
| GET | `/:id` | Get article (increments view_count) |
| POST | `/` | Create article |
| PUT | `/:id` | Update article |
| DELETE | `/:id` | Delete article |

### Dashboard (`/api/v1/dashboard`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/stats` | Statistics (totals, items_by_category, users_by_role) |
| GET | `/recent-activities` | Last 10 activities with user info |
| GET | `/overview` | System overview (db_size, storage, uptime, etc.) |

### Upload (`/api/v1/upload`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/image` | Upload image (field name: `file`, max 5MB) |
| POST | `/file` | Upload file (field name: `file`, max 50MB) |

### Settings (`/api/v1/settings`) — Admin only
| Method | Path | Description |
|--------|------|-------------|
| GET | `/site` | Get site settings |
| PUT | `/site` | Update site settings |
| GET | `/mail` | Get mail settings (password masked) |
| PUT | `/mail` | Update mail settings (skips masked password) |
| POST | `/mail/test` | Send test email (mock) |
| POST | `/cache/clear` | Clear cache (mock) |
| GET | `/logs` | Paginated activity logs |

## Field Naming Convention
- API uses **snake_case** for all field names
- Date fields: `created_at`, `updated_at`, `last_login`, `published_at`, `due_date`
- Boolean fields stored as strings: `"true"` / `"false"`

## Default Admin Account
- Username: `admin`
- Password: `admin123`
