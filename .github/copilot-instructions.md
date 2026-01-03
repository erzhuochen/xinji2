# XinJi (心迹) - AI Coding Guidelines

## Architecture Overview

XinJi is a mental health journaling app with AI emotion analysis. **Dual-database architecture**:
- **MySQL** (MyBatis-Plus): User accounts, diary metadata, orders, payments → `mapper/` package
- **MongoDB** (Spring Data): Diary content, AI analysis results, weekly reports → `repository/mongo/` package

```
xinji-backend/com.xinji/
├── mapper/           # MySQL repositories (MyBatis-Plus BaseMapper)
├── repository/mongo/ # MongoDB repositories (Spring Data)
├── entity/           # MySQL entities (@TableName("t_*"))
├── entity/mongo/     # MongoDB documents (@Document)
├── service/impl/     # Business logic
└── security/         # JWT auth, SecurityContext
```

## Critical Patterns

### Database Layer Separation
- **MySQL entities**: Use `@TableName("t_user")` prefix, `@TableId(type = IdType.INPUT)` for UUID PKs
- **MongoDB documents**: POJOs with `@Document` in `entity/mongo/` package
- **NEVER mix**: MyBatis mappers scan `com.xinji.mapper`, MongoDB repos in `com.xinji.repository.mongo`

```java
// MySQL - mapper package
@Mapper
public interface UserRepository extends BaseMapper<User> { }

// MongoDB - repository.mongo package  
public interface DiaryContentRepository extends MongoRepository<DiaryContent, String> { }
```

### Security Context
All controllers get current user via `SecurityContext` (injected):
```java
String userId = securityContext.getCurrentUserId();
```

### API Response Pattern
Always use `ApiResponse<T>` wrapper:
```java
return ApiResponse.success("操作成功", data);
return ApiResponse.success(data);  // message optional
```

### Sensitive Data Encryption
Diary content encrypted with `AESUtil` (AES-CBC-PKCS5Padding):
```java
String encrypted = aesUtil.encrypt(plainText);
String decrypted = aesUtil.decrypt(cipherText);
```

### Null Safety in MongoDB Documents
MongoDB documents may have null fields. Always check before using:
```java
// ❌ Bad - NullPointerException risk
totalIntensity += ar.getEmotionIntensity();

// ✅ Good - null check with default
double intensity = ar.getEmotionIntensity() != null ? ar.getEmotionIntensity() : 0.5;
```

## Frontend Patterns

### API Layer
- Base URL: `/api` (proxied to backend at :12380)
- All requests use `src/api/request.ts` axios instance with JWT interceptor
- Types in `src/types/index.ts` must match backend DTOs

### Router Paths
```typescript
/diary          → DiaryList
/diary/:id      → DiaryDetail (NOT /diary/detail/:id)
/diary/edit/:id → DiaryEdit
```

### Store Pattern
Use Pinia stores with composition API:
```typescript
const userStore = useUserStore()
await userStore.fetchUserProfile()  // NOT fetchUserInfo()
```

### Week Calculation (ISO Week)
Use `isoWeek` plugin for Monday-Sunday weeks:
```typescript
import isoWeek from 'dayjs/plugin/isoWeek'
dayjs.extend(isoWeek)
dayjs().startOf('isoWeek')  // Monday, NOT startOf('week') which is Sunday
```

## Common Pitfalls

1. **Order entity**: PK is `id` (maps to `id` column), with separate `orderNo` for business order number
2. **MySQL table prefix**: All tables use `t_` prefix (t_user, t_diary, t_order)
3. **MongoDB collections**: No prefix (diary_contents, analysis_results)
4. **NOT NULL fields**: Always set `orderNo` when creating orders
5. **Soft delete**: MySQL entities need `@TableLogic private Integer deleted;`

## Development Commands

```bash
# Backend (from xinji-backend/)
./mvnw spring-boot:run

# Frontend (from xinji-frontend/)
npm run dev          # Dev server at :5173
npm run build        # Production build

# Full stack with Docker
docker-compose up -d
```

## Key Configuration Files
- `xinji-backend/src/main/resources/application.yml` - DB connections, AI keys
- `xinji-backend/docs/init.sql` - MySQL schema
- `xinji-backend/docs/init-mongo.js` - MongoDB indexes
- `xinji-frontend/vite.config.ts` - API proxy to localhost:12380
