# سامانه رزرو بلیط سینما

بک‌اند Spring Boot با Java 25، Maven و MySQL. احراز هویت مبتنی بر Session،
مدیریت فیلم، خرید و لغو بلیط و آمار فروش پیاده‌سازی شده‌اند.

## اجرا

Java 25، Maven و MySQL باید نصب باشند. اگر اسکیمای `cinema_booking` وجود ندارد،
در MySQL دستور `CREATE DATABASE cinema_booking CHARACTER SET utf8mb4;` را اجرا کنید. تنظیمات اتصال در
`src/main/resources/application.properties` قرار دارند. در PowerShell داخل
پوشه پروژه:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.1'
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" spring-boot:run
```

اگر پورت 8080 در حال استفاده است، می‌توان دستور را با پورت دیگری اجرا کرد:

```powershell
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"
```

در IntelliJ، Project SDK و Maven Runner JDK را روی 25 بگذارید و کلاس
`CinemaBookingApplication` را اجرا کنید. Hibernate جدول‌ها را ایجاد یا به‌روز
می‌کند. Seeder فقط زمانی اجرا می‌شود که هر چهار جدول کاملاً خالی باشند؛ این
کار جلوی بازگشت داده‌های ویرایش‌شده یا حذف‌شده در اجرای بعدی را می‌گیرد.

داده اولیه: ادمین `admin` با رمز `admin123`، کاربر `user1` با رمز `user123`،
یک سالن با ۵۰ صندلی، سه فیلم و یک بلیط فعال نمونه. رمز کاربران BCrypt است.
پیش از اشتراک‌گذاری پروژه، رمز اتصال محلی MySQL را از فایل تنظیمات خارج کنید.

## آزمایش با Swagger

پس از اجرا به `http://localhost:8080/swagger-ui/index.html` بروید. اگر از
پورت دیگری استفاده می‌کنید، همان پورت را در آدرس بگذارید. سند OpenAPI در
`/v3/api-docs` در دسترس است.

1. در بخش Authentication درخواست `GET /api/auth/csrf` را با **Try it out**
   و **Execute** اجرا کنید. مرورگر کوکی `XSRF-TOKEN` را دریافت می‌کند.
2. `POST /api/auth/login` را با `admin` و `admin123` اجرا کنید. مرورگر کوکی
   `JSESSIONID` را نگه می‌دارد. حساب عادی `user1` / `user123` است.
3. **بعد از ورود دوباره** `GET /api/auth/csrf` را اجرا کنید؛ توکن قبلی هنگام
   ورود باطل می‌شود. Swagger مقدار کوکی جدید را برای درخواست‌های POST، PUT و
   DELETE در هدر `X-XSRF-TOKEN` می‌فرستد.
4. `GET /api/auth/me` و عملیات دلخواه را اجرا کنید. عملیات بخش Admin movies
   فقط برای ADMIN مجازند. خروج با `POST /api/auth/logout` انجام می‌شود؛ پس از
   خروج برای ورود یا ثبت‌نام مجدد، ابتدا توکن CSRF تازه بگیرید.

در Postman نیز ابتدا `GET /api/auth/csrf` را بزنید، کوکی‌ها را نگه دارید و
مقدار `data.token` پاسخ را در هدر `X-XSRF-TOKEN` درخواست‌های POST، PUT و
DELETE ارسال کنید. پس از ورود یا خروج، توکن جدید بگیرید. CSRF حتی برای ورود
و ثبت‌نام نیز الزامی است.

## APIها

| روش | مسیر | دسترسی | کاربرد |
|---|---|---|---|
| GET | `/api/auth/csrf` | عمومی | دریافت توکن CSRF |
| POST | `/api/auth/register` | عمومی | ثبت‌نام با نقش USER |
| POST | `/api/auth/login` | عمومی | ورود و ایجاد Session |
| POST | `/api/auth/logout` | واردشده | خروج و باطل‌کردن Session |
| GET | `/api/auth/me` | واردشده | اطلاعات کاربر جاری |
| GET | `/api/movies` | عمومی | فهرست فیلم‌ها |
| GET | `/api/movies/{id}` | عمومی | جزئیات فیلم |
| GET | `/api/movies/search?title=...` | عمومی | جستجوی نسبی عنوان، بدون حساسیت به حروف بزرگ و کوچک |
| GET | `/api/movies/{id}/available-seats` | عمومی | شماره صندلی‌های آزاد |
| POST | `/api/admin/movies` | ADMIN | افزودن فیلم |
| PUT | `/api/admin/movies/{id}` | ADMIN | ویرایش فیلم |
| DELETE | `/api/admin/movies/{id}` | ADMIN | حذف منطقی فیلم |
| POST | `/api/tickets/book` | USER | خرید بلیط با شناسه فیلم و شماره صندلی |
| GET | `/api/tickets/my-tickets` | USER | بلیط‌های فعال و لغوشده کاربر جاری |
| DELETE | `/api/tickets/{id}` | USER | لغو بلیط متعلق به کاربر |
| GET | `/api/admin/tickets/stats` | ADMIN | تعداد فروش و درآمد کل و به تفکیک فیلم |

نمونه بدنه ساخت یا ویرایش فیلم؛ `cinemaHallId` باید شناسه سالن موجود باشد و
قیمت به تومان است:

```json
{
  "title": "A New Film",
  "genre": "Drama",
  "durationMinutes": 120,
  "releaseDate": "2026-01-01",
  "description": "A sample film",
  "price": 120000,
  "cinemaHallId": 1
}
```

نمونه بدنه خرید بلیط:

```json
{"movieId": 1, "seatNumber": 12}
```

برای آزمایش، با `user1` وارد شوید، توکن CSRF تازه بگیرید و خرید را انجام
دهید. شناسه بلیط در `data.id` برمی‌گردد؛ همان را برای لغو استفاده کنید.
خرید مجدد یک صندلی فعال پاسخ `409` با پیام فارسی می‌دهد. لغو، صندلی را آزاد
می‌کند ولی بلیط لغوشده در تاریخچه باقی می‌ماند. برای دیدن آمار، خارج شوید و
با `admin` وارد شوید. آمار فقط بلیط‌های فعال را می‌شمارد، قیمت زمان خرید را
جمع می‌کند و شامل فیلم‌های حذف‌شده نیز هست تا سابقه فروش آن‌ها حفظ شود.

پاسخ‌های موفق قالب `timestamp`، `status`، `message` و `data` دارند. خطاها
طبق صورت پروژه شامل `timestamp`، `status`، `error`، `message` و `path` هستند.
شماره صندلی از ۱ تا ظرفیت سالن است و فقط بلیط‌های `ACTIVE` صندلی را اشغال
می‌کنند. هنگام تغییر سالن، اگر صندلی فروخته‌شده‌ای خارج از ظرفیت سالن جدید
باشد، درخواست با خطای 409 رد می‌شود.

## حذف منطقی و Auditing

هر Entity از `BaseEntity` ارث می‌برد. زمان و نام کاربر ایجاد و ویرایش با JPA
Auditing از کاربر احراز‌شده گرفته می‌شود؛ برای عملیات بدون کاربر مقدار `system`
ثبت می‌شود. داده‌های قدیمی، زمان و نام ایجادکننده پیشین را ندارند.

`@SQLRestriction` ردیف‌های دارای `deleted_at` را از پرس‌وجوهای Entity مخفی
می‌کند. در حذف فیلم، `deleted_at` و `deleted_by` ثبت و `active` غیرفعال
می‌شود؛ بلیط‌های موجود در دیتابیس باقی می‌مانند. این محدودیت جلوی حذف فیزیکی
از طریق `repository.delete` را نمی‌گیرد؛ سرویس‌های بعدی باید از `markDeleted()`
استفاده کنند. وضعیت لغو بلیط، جدا از حذف منطقی است. تاریخچه و آمار با خواندن
کنترل‌شده جدول فیلم‌ها، بلیط فیلم‌های حذف‌شده را نیز حفظ می‌کنند.

## همزمانی و ایمنی صندلی

برای هر فیلم، ستون محاسبه‌شده `active_seat_number` فقط برای بلیط فعال و
حذف‌نشده شماره صندلی را نگه می‌دارد. محدودیت یکتای دیتابیس روی ترکیب این
ستون و شناسه فیلم مانع ثبت دو بلیط فعال می‌شود؛ در بلیط لغوشده مقدار ستون
`NULL` است تا صندلی دوباره فروخته شود. `@Version` و قفل خوش‌بینانه هنگام
خواندن فیلم، تغییر همزمان فیلم را بررسی می‌کنند. اگر ستون محاسبه‌شده یا
محدودیت یکتا در دیتابیس وجود نداشته باشد، بررسی شروع برنامه مانع اجرای آن
می‌شود. در صورت وجود داده تکراری از قبل، قبل از راه‌اندازی باید آن رکوردها
اصلاح شوند.

## اجرای تست‌ها

با همان تنظیم `JAVA_HOME`، دستور زیر را از پوشه پروژه اجرا کنید:

```powershell
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" test
```

تست‌ها به‌صورت پیش‌فرض از H2 در حافظه استفاده می‌کنند و داده‌های MySQL برنامه
را تغییر نمی‌دهند. برای اجرای فقط تست همزمانی `ExecutorService`، گزینه
`-Dtest=TicketConcurrencyTest` را قبل از `test` به دستور Maven اضافه کنید.
دو تراکنش با `CyclicBarrier` همزمان شروع می‌شوند و تست بررسی می‌کند که دقیقاً
یک خرید موفق شود و محدودیت دیتابیس نیز درج تکراری مستقیم را رد کند. این تست‌ها
روی یک اسکیمای مستقل MySQL نیز قابل اجرا هستند؛ هیچ‌گاه تنظیم آزمایشی
`create-drop` را به اسکیمای اصلی متصل نکنید.

## Functional Programming

- `BookingRule`: رابط تابعی اختصاصی در سرویس بلیط؛ دو Lambda قوانین ظرفیت و
  آزادبودن صندلی را بررسی می‌کنند و فهرست قوانین با `forEach` اجرا می‌شود.
- `Predicate<Movie>` در `MovieService.list` برای فیلتر عنوان فیلم‌ها استفاده
  شده است؛ چون شرط جستجو را می‌توان مستقل از مرحله مرتب‌سازی تغییر داد.
- `Comparator` و Lambda در همان متد فیلم‌ها را بر اساس عنوان مرتب می‌کنند.
- Stream و Method Reference برای فیلتر و تبدیل فیلم‌ها، محاسبه صندلی‌های آزاد
  و جمع تعداد و درآمد آمار استفاده شده‌اند؛ این عملیات روی مجموعه‌ها را خوانا
  و قابل ترکیب می‌کنند.

پیام‌های خطای API و اعتبارسنجی فارسی هستند و locale پیش‌فرض وب `fa` است.
فیلد `error` همچنان نام استاندارد وضعیت HTTP مانند `CONFLICT` را نگه می‌دارد.

## اجرا با Docker Compose

Docker Desktop و افزونه Compose باید نصب و Docker Desktop روشن باشد. در PowerShell، داخل پوشه پروژه:

```powershell
docker compose up -d --build
docker compose ps
docker compose logs -f app
```

اولین ساخت ممکن است به علت دریافت imageها و وابستگی‌ها چند دقیقه طول بکشد. پس از شروع موفق، Swagger در `http://localhost:8082/swagger-ui/index.html` و OpenAPI در `http://localhost:8082/v3/api-docs` در دسترس است. Ctrl+C فقط نمایش لاگ را متوقف می‌کند. برای توقف کانتینرها:

```powershell
docker compose down
```

MySQL کانتینری مستقل از MySQL ویندوز است و پورت دیتابیس روی میزبان باز نمی‌شود. داده‌ها در volume به نام `mysql_data` باقی می‌مانند. دستور `docker compose down -v` علاوه بر کانتینرها **تمام داده‌های دیتابیس کانتینری را حذف می‌کند**؛ فقط برای بازنشانی کامل از آن استفاده کنید.

پورت پیش‌فرض برنامه 8082 است. اگر اشغال بود، قبل از اجرا در همان PowerShell مثلاً `$env:APP_PORT='8083'` را تنظیم کنید و از پورت 8083 استفاده کنید. رمزهای پیش‌فرض Compose صرفاً برای توسعه هستند. پیش از اولین ساخت volume می‌توانید با `$env:MYSQL_PASSWORD='...'` و `$env:MYSQL_ROOT_PASSWORD='...'` آن‌ها را تغییر دهید؛ تغییر متغیرها پس از ساخته‌شدن volume، رمز دیتابیس موجود را خودکار تغییر نمی‌دهد. فایل تنظیمات محلی دارای رمز MySQL داخل image کپی نمی‌شود و تنظیمات مستقل کانتینر در `docker/application.properties` قرار دارند.

در صورت آماده‌نشدن برنامه، `docker compose ps` و `docker compose logs db app` را بررسی کنید. برنامه منتظر سالم‌شدن MySQL می‌ماند. خطای پورت را با تغییر `APP_PORT` و خطای دریافت image یا وابستگی را با بررسی اتصال شبکه برطرف کنید.

## مجموعه Postman

فایل `postman/Cinema-Booking.postman_collection.json` را با **Import** وارد Postman کنید. متغیر collection به نام `baseUrl` برای Compose برابر `http://localhost:8082` است؛ برای اجرای محلی آن را به `http://localhost:8080` یا پورت انتخابی تغییر دهید. Cookie Jar باید فعال باشد و همه درخواست‌ها از یک hostname استفاده کنند.

1. **Get CSRF token** را اجرا کنید؛ اسکریپت پاسخ توکن را در `csrfToken` ذخیره می‌کند.
2. **Login USER** و سپس دوباره **Get CSRF token** را اجرا کنید. Postman کوکی نشست را نگه می‌دارد.
3. فیلم‌ها و صندلی‌های آزاد را ببینید. **Book ticket** را اجرا کنید؛ شناسه بلیط در `ticketId` ذخیره می‌شود. همان درخواست را دوباره بزنید تا خطای 409 را ببینید، سپس با **Cancel ticket** بلیط را لغو کنید. اگر صندلی 12 آزاد نیست، شماره آزاد دیگری در بدنه انتخاب کنید.
4. برای مدیر، به ترتیب **Logout**، **Get CSRF token**، **Login ADMIN** و دوباره **Get CSRF token** را اجرا کنید. آمار و عملیات فیلم اکنون قابل آزمایش‌اند. ابتدا فیلم بسازید تا شناسه آن در `adminMovieId` ذخیره شود؛ بعد ویرایش و حذف را اجرا کنید.

درخواست ثبت‌نام نام و ایمیل بر اساس زمان می‌سازد؛ در صورت تکرار در یک ثانیه نام و ایمیل جدید بدهید. متغیر `movieId` برای جزئیات و صندلی‌ها استفاده می‌شود؛ بدنه خرید مستقل است و `movieId` و `seatNumber` آن را نیز در صورت نیاز تغییر دهید. شناسه‌ها در دیتابیس‌های غیربکر ممکن است با نمونه‌ها فرق داشته باشند.

## نمونه درخواست و پاسخ API

مسیرها به آدرس پایه اضافه می‌شوند. درخواست‌های تغییر داده به هدر `X-XSRF-TOKEN` و کوکی CSRF نیاز دارند؛ عملیات محافظت‌شده همچنین به کوکی نشست نیاز دارند. زمان‌ها و شناسه‌ها نمونه‌اند. قالب پاسخ موفق:

```json
{"timestamp":"2026-10-30T10:00:00","status":200,"message":"Current user","data":{"id":2,"username":"user1","email":"user1@example.com","role":"USER"}}
```

در جدول زیر برای اختصار تنها مقدار نمونه `data` نمایش داده شده است؛ `status` و `message` نیز در پوشش پاسخ قرار دارند. خروجی فهرست‌ها و صندلی‌ها برای اختصار به شکل توضیحی نمایش داده شده است.

| روش و مسیر | بدنه یا پارامتر نمونه | وضعیت و نمونه data |
|---|---|---|
| GET /api/auth/csrf | — | 200؛ `{"headerName":"X-XSRF-TOKEN","token":"..."}` |
| POST /api/auth/register | `{"username":"newuser","email":"newuser@example.com","password":"password123"}` | 201؛ `{"id":3,"username":"newuser","email":"newuser@example.com","role":"USER"}` |
| POST /api/auth/login | `{"username":"user1","password":"user123"}` | 200؛ شیء کاربر مانند نمونه بالا |
| GET /api/auth/me | — | 200؛ شیء کاربر جاری مانند نمونه بالا |
| POST /api/auth/logout | — | 200؛ `null` |
| GET /api/movies | — | 200؛ آرایه اشیای فیلم؛ نمونه فیلم در ادامه |
| GET /api/movies/search?title=Interstellar | `title=Interstellar` | 200؛ آرایه فیلم‌های مطابق عنوان |
| GET /api/movies/1 | — | 200؛ شیء فیلم زیر |
| GET /api/movies/1/available-seats | — | 200؛ `{"movieId":1,"availableSeats":[2,3,...,50]}`؛ صندلی ۱ در داده اولیه فروخته شده است |
| POST /api/admin/movies | بدنه فیلم در بخش APIها | 201؛ شیء فیلم ساخته‌شده شامل `id` و `cinemaHall` |
| PUT /api/admin/movies/4 | همان بدنه فیلم با عنوان جدید | 200؛ شیء فیلم ویرایش‌شده |
| DELETE /api/admin/movies/4 | — | 200؛ `null` |
| POST /api/tickets/book | `{"movieId":1,"seatNumber":12}` | 201؛ شیء بلیط زیر |
| GET /api/tickets/my-tickets | — | 200؛ آرایه بلیط‌های فعال و لغوشده |
| DELETE /api/tickets/2 | — | 200؛ `null` |
| GET /api/admin/tickets/stats | — | 200؛ شیء آمار زیر |

نمونه فیلم از داده اولیه:

```json
{"id":1,"title":"The Shawshank Redemption","genre":"Drama","durationMinutes":142,"releaseDate":"1994-09-23","description":"A story of hope and friendship in prison.","price":120000,"cinemaHall":{"id":1,"name":"Hall 1","capacity":50}}
```

نمونه `data` برای بلیط خریداری‌شده و آمار فروش بر اساس داده اولیه، پیش از سایر خریدها؛ درآمد به تومان است:

```json
{"id":2,"movieId":1,"movieTitle":"The Shawshank Redemption","seatNumber":12,"purchaseDate":"2026-10-30T10:00:00","price":120000,"status":"ACTIVE","cancelledAt":null}
```

```json
{"totalTickets":1,"totalRevenue":120000,"movies":[{"movieId":1,"movieTitle":"The Shawshank Redemption","activeTicketCount":1,"totalRevenue":120000},{"movieId":2,"movieTitle":"Interstellar","activeTicketCount":0,"totalRevenue":0},{"movieId":3,"movieTitle":"Inception","activeTicketCount":0,"totalRevenue":0}]}
```

نمونه خطای خرید مجدد صندلی:

```json
{"timestamp":"2026-10-30T10:00:00","status":409,"error":"CONFLICT","message":"صندلی شماره 12 قبلاً فروخته شده است","path":"/api/tickets/book"}
```

## اجرای تست همزمانی و تهیه اسکرین‌شات

این تست در `src/test/java/com/example/cinemabooking/TicketConcurrencyTest.java` قرار دارد. در PowerShell از پوشه پروژه:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.1'
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" "-Dtest=TicketConcurrencyTest" test
```

مسیر JDK را در صورت تفاوت تغییر دهید. اگر تنظیمات Maven سالم است، `mvn -Dtest=TicketConcurrencyTest test` کافی است. تست به‌طور پیش‌فرض از H2 حافظه‌ای استفاده می‌کند، نه دیتابیس کانتینر یا MySQL محلی. تست دو کاربر و یک فیلم آزمایشی می‌سازد؛ `ExecutorService` دو تراکنش مستقل راه می‌اندازد و `CyclicBarrier` تضمین می‌کند هر دو ابتدا صندلی ۱۲ را آزاد ببینند. سپس بررسی می‌شود دقیقاً یک خرید موفق، یک `SeatAlreadyBookedException` و یک بلیط فعال وجود دارد؛ همچنین درج مستقیم رکورد تکراری باید با محدودیت یکتای دیتابیس رد شود. خطای SQL مربوط به درج عمدی تکراری ممکن است در لاگ ظاهر شود و با موفقیت تست تناقضی ندارد.

پس از پایان موفق، خط `CONCURRENCY RESULT: 2 attempts, 1 successful booking, 1 rejected, 1 active ticket; duplicate DB insert rejected.` و سپس `Tests run: 1, Failures: 0, Errors: 0` و `BUILD SUCCESS` را در خروجی ببینید. پنجره ترمینال را طوری تنظیم کنید که این خطوط در تصویر دیده شوند و با Win+Shift+S اسکرین‌شات بگیرید. جزئیات نتیجه در `target/surefire-reports/com.example.cinemabooking.TicketConcurrencyTest.txt` نیز ثبت می‌شود. این تست اجرای همزمان روی H2 را نشان می‌دهد؛ برای اثبات رفتار MySQL نیز باید همین تست را روی یک اسکیمای آزمایشی مستقل MySQL اجرا کنید، نه دیتابیس اصلی.
