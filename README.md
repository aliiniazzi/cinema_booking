<div dir="rtl" align="right">

# سامانه رزرو بلیط سینما

این پروژه یه بک‌اند برای رزرو بلیط سینماست که با Java 25، Spring Boot، Maven و MySQL نوشته شده. کاربر می‌تونه فیلم‌ها رو ببینه و بلیط بخره یا لغو کنه؛ مدیر هم فیلم‌ها و آمار فروش رو مدیریت می‌کنه. ورود به حساب با Session انجام می‌شه.

## اجرا بدون Docker

Java 25، Maven و MySQL باید نصب باشن. اگه اسکیمای `cinema_booking` وجود نداره،
توی MySQL دستور `CREATE DATABASE cinema_booking CHARACTER SET utf8mb4;` رو اجرا کن. تنظیمات اتصال در
`src/main/resources/application.properties` قرار دارن. توی PowerShell داخل
پوشه پروژه:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.1'
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" spring-boot:run
```

اگه پورت 8080 اشغال شده، می‌تونی برنامه رو روی یه پورت دیگه اجرا کنی:

```powershell
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"
```

توی IntelliJ، Project SDK و Maven Runner JDK رو روی 25 بذار و کلاس
`CinemaBookingApplication` رو اجرا کن. Hibernate جدول‌ها رو ایجاد یا به‌روز
می‌کنه. Seeder فقط وقتی اجرا می‌شه که هر چهار جدول کاملاً خالی باشن؛ این
کار جلوی بازگشت داده‌های ویرایش‌شده یا حذف‌شده توی اجرای بعدی رو می‌گیره.

داده اولیه: ادمین `admin` با رمز `admin123`، کاربر `user1` با رمز `user123`،
یک سالن با ۵۰ صندلی، سه فیلم و یک بلیط فعال نمونه. رمز کاربران BCrypt هست.
قبل از اشتراک‌گذاری پروژه، رمز اتصال محلی MySQL رو از فایل تنظیمات خارج کن.

## آزمایش با Swagger

بعد از اجرا به `http://localhost:8080/swagger-ui/index.html` برو. اگه از
پورت دیگه‌ای استفاده می‌کنی، همون پورت رو توی آدرس بذار. سند OpenAPI در
`/v3/api-docs` در دسترسه.

1. توی بخش Authentication درخواست `GET /api/auth/csrf` رو با **Try it out**
   و **Execute** اجرا کن. مرورگر کوکی `XSRF-TOKEN` رو دریافت می‌کنه.
2. `POST /api/auth/login` رو با `admin` و `admin123` اجرا کن. مرورگر کوکی
   `JSESSIONID` رو نگه می‌داره. حساب عادی `user1` / `user123` هست.
3. **بعد از ورود دوباره** `GET /api/auth/csrf` رو اجرا کن؛ توکن قبلی هنگام
   ورود باطل می‌شه. Swagger مقدار کوکی جدید رو برای درخواست‌های POST، PUT و
   DELETE توی هدر `X-XSRF-TOKEN` می‌فرسته.
4. `GET /api/auth/me` و عملیات دلخواه رو اجرا کن. عملیات بخش Admin movies
   فقط برای ADMIN مجازن. خروج با `POST /api/auth/logout` انجام می‌شه؛ بعد از
   خروج برای ورود یا ثبت‌نام مجدد، اول توکن CSRF تازه بگیر.

در Postman هم اول `GET /api/auth/csrf` رو بزن، کوکی‌ها رو نگه دار و
مقدار `data.token` پاسخ رو توی هدر `X-XSRF-TOKEN` درخواست‌های POST، PUT و
DELETE بفرست. بعد از ورود یا خروج، توکن جدید بگیر. CSRF حتی برای ورود
و ثبت‌نام هم الزامی هست.

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

نمونه بدنه ساخت یا ویرایش فیلم؛ `cinemaHallId` باید شناسه یه سالن موجود باشه و
قیمت به تومنه:

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

برای آزمایش، با `user1` وارد شو، توکن CSRF تازه بگیر و خرید رو انجام
بده. شناسه بلیط توی `data.id` برمی‌گرده؛ همان رو برای لغو استفاده کن.
خرید مجدد یک صندلی فعال پاسخ `409` با پیام فارسی می‌ده. لغو، صندلی رو آزاد
می‌کنه ولی بلیط لغوشده توی تاریخچه باقی می‌مونه. برای دیدن آمار، خارج شو و
با `admin` وارد شو. آمار فقط بلیط‌های فعال رو می‌شمره، قیمت زمان خرید رو
جمع می‌کنه و شامل فیلم‌های حذف‌شده هم هست تا سابقه فروش آن‌ها حفظ بشه.

پاسخ‌های موفق قالب `timestamp`، `status`، `message` و `data` دارن. خطاها
طبق صورت پروژه شامل `timestamp`، `status`، `error`، `message` و `path` هستن.
شماره صندلی از ۱ تا ظرفیت سالن هست و فقط بلیط‌های `ACTIVE` صندلی رو اشغال
می‌کنن. هنگام تغییر سالن، اگه صندلی فروخته‌شده‌ای خارج از ظرفیت سالن جدید
باشه، درخواست با خطای 409 رد می‌شه.

## حذف منطقی و Auditing

هر Entity از `BaseEntity` ارث می‌بره. زمان و نام کاربر ایجاد و ویرایش با JPA
Auditing از کاربر احراز‌شده گرفته می‌شه؛ برای عملیات بدون کاربر مقدار `system`
ثبت می‌شه. زمان و نام ایجادکننده برای داده‌های قدیمی ثبت نشده.

`@SQLRestriction` ردیف‌های دارای `deleted_at` رو از پرس‌وجوهای Entity مخفی
می‌کنه. توی حذف فیلم، `deleted_at` و `deleted_by` ثبت و `active` غیرفعال
می‌شه؛ بلیط‌های موجود توی دیتابیس باقی می‌مونن. این محدودیت جلوی حذف فیزیکی
با `repository.delete` رو نمی‌گیره؛ سرویس‌های بعدی باید از `markDeleted()`
استفاده کنن. وضعیت لغو بلیط، جدا از حذف منطقی هست. تاریخچه و آمار با خوندن
جدول فیلم‌ها، بلیط فیلم‌های حذف‌شده رو هم حفظ می‌کنن.

## همزمانی و ایمنی صندلی

برای هر فیلم، ستون محاسبه‌شده `active_seat_number` فقط برای بلیط فعال و
حذف‌نشده شماره صندلی رو نگه می‌داره. محدودیت یکتای دیتابیس روی ترکیب این
ستون و شناسه فیلم مانع ثبت دو بلیط فعال می‌شه؛ توی بلیط لغوشده مقدار ستون
`NULL` هست تا صندلی دوباره فروخته بشه. `@Version` و قفل خوش‌بینانه هنگام
خواندن فیلم، تغییر همزمان فیلم رو بررسی می‌کنن. اگه ستون محاسبه‌شده یا
محدودیت یکتا توی دیتابیس وجود نداشته باشه، برنامه موقع شروع جلوی اجرا رو می‌گیره. در صورت وجود داده تکراری از قبل، قبل از راه‌اندازی باید اون رکوردها
اصلاح بشن.

## اجرای تست‌ها

با همون تنظیم `JAVA_HOME`، دستور زیر رو از پوشه پروژه اجرا کن:

```powershell
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" test
```

تست‌ها به‌طور پیش‌فرض از H2 توی حافظه استفاده می‌کنن و داده‌های MySQL برنامه
رو تغییر نمی‌دن. برای اجرای فقط تست همزمانی `ExecutorService`، گزینه
`-Dtest=TicketConcurrencyTest` رو قبل از `test` به دستور Maven اضافه کن.
توی این تست، `ExecutorService` دو تراکنش برای خرید یه صندلی اجرا می‌کنه و `CyclicBarrier` باعث می‌شه هر دو اول صندلی رو آزاد ببینن. تست چک می‌کنه فقط یک خرید موفق بشه، درخواست دیگه خطای صندلی فروخته‌شده بگیره و فقط یک بلیط فعال باقی بمونه. محدودیت دیتابیس هم باید درج مستقیم بلیط تکراری رو رد کنه. ممکنه خطای SQL مربوط به این تلاش عمدی توی لاگ دیده بشه. این تست‌ها
روی یک اسکیمای مستقل MySQL هم قابل اجرا هستن؛ هیچ‌گاه تنظیم آزمایشی
`create-drop` رو به اسکیمای اصلی وصل نکن.

## Functional Programming

- `BookingRule`: رابط تابعی اختصاصی توی سرویس بلیط؛ دو Lambda قوانین ظرفیت و
  آزادبودن صندلی رو بررسی می‌کنن و فهرست قوانین با `forEach` اجرا می‌شه.
- `Predicate<Movie>` توی `MovieService.list` برای فیلتر عنوان فیلم‌ها استفاده
  شده؛ چون شرط جستجو رو می‌شه مستقل از مرحله مرتب‌سازی تغییر داد.
- `Comparator` و Lambda توی همون متد فیلم‌ها رو بر اساس عنوان مرتب می‌کنن.
- Stream و Method Reference برای فیلتر و تبدیل فیلم‌ها، محاسبه صندلی‌های آزاد
  و جمع تعداد و درآمد آمار استفاده شدن؛ این روش کار با مجموعه‌ها رو ساده‌تر می‌کنه.

پیام‌های خطای API و اعتبارسنجی فارسی هستن و locale پیش‌فرض وب `fa` هست.
فیلد `error` همچنان نام استاندارد وضعیت HTTP مانند `CONFLICT` رو نگه می‌داره.

## اجرا با Docker Compose

Docker Desktop و افزونه Compose باید نصب و Docker Desktop روشن باشه. توی PowerShell، داخل پوشه پروژه:

```powershell
docker compose up -d --build
docker compose ps
docker compose logs -f app
```

اولین ساخت ممکنه به علت دریافت imageها و وابستگی‌ها چند دقیقه طول بکشه. بعد از شروع موفق، Swagger توی `http://localhost:8082/swagger-ui/index.html` و OpenAPI توی `http://localhost:8082/v3/api-docs` در دسترسه. Ctrl+C فقط نمایش لاگ رو متوقف می‌کنه. برای توقف کانتینرها:

```powershell
docker compose down
```

MySQL کانتینری مستقل از MySQL ویندوز هست و پورت دیتابیس روی میزبان باز نمی‌شه. داده‌ها توی volume به نام `mysql_data` باقی می‌مونن. دستور `docker compose down -v` علاوه بر کانتینرها **تمام داده‌های دیتابیس کانتینری رو حذف می‌کنه**؛ فقط برای بازنشانی کامل ازش استفاده کن.

پورت پیش‌فرض برنامه 8082 هست. اگه اشغال بود، قبل از اجرا توی همون PowerShell مثلاً `$env:APP_PORT='8083'` رو تنظیم کن و از پورت 8083 استفاده کن. رمزهای پیش‌فرض Compose صرفاً برای توسعه هستن. قبل از اولین ساخت volume می‌تونی با `$env:MYSQL_PASSWORD='...'` و `$env:MYSQL_ROOT_PASSWORD='...'` تغییرشون بدی؛ تغییر متغیرها بعد از ساخته‌شدن volume، رمز دیتابیس موجود رو خودکار تغییر نمی‌ده. فایل تنظیمات محلی دارای رمز MySQL داخل image کپی نمی‌شه و تنظیمات مستقل کانتینر توی `docker/application.properties` قرار دارن.

در صورت آماده‌نشدن برنامه، `docker compose ps` و `docker compose logs db app` رو بررسی کن. برنامه منتظر سالم‌شدن MySQL می‌مونه. خطای پورت رو با تغییر `APP_PORT` و خطای دریافت image یا وابستگی رو با بررسی اتصال شبکه برطرف کن.

## مجموعه Postman

فایل `postman/Cinema-Booking.postman_collection.json` رو با **Import** وارد Postman کن. متغیر collection به نام `baseUrl` برای Compose برابر `http://localhost:8082` هست؛ برای اجرای محلی اون رو به `http://localhost:8080` یا پورت انتخابی تغییر بده. Cookie Jar باید فعال باشه و همه درخواست‌ها از یک hostname استفاده کنن.

1. **Get CSRF token** رو اجرا کن؛ اسکریپت پاسخ توکن رو توی `csrfToken` ذخیره می‌کنه.
2. **Login USER** و بعد دوباره **Get CSRF token** رو اجرا کن. Postman کوکی نشست رو نگه می‌داره.
3. فیلم‌ها و صندلی‌های آزاد رو ببین. **Book ticket** رو اجرا کن؛ شناسه بلیط توی `ticketId` ذخیره می‌شه. همون درخواست رو دوباره بزن تا خطای 409 رو ببین، بعد با **Cancel ticket** بلیط رو لغو کن. اگه صندلی 12 آزاد نیست، شماره آزاد دیگه‌ای توی بدنه انتخاب کن.
4. برای مدیر، به ترتیب **Logout**، **Get CSRF token**، **Login ADMIN** و دوباره **Get CSRF token** رو اجرا کن. آمار و عملیات فیلم حالا قابل تست هستن. اول فیلم بساز تا شناسه‌اش توی `adminMovieId` ذخیره بشه؛ بعد ویرایش و حذف رو اجرا کن.

درخواست ثبت‌نام نام و ایمیل بر اساس زمان می‌سازه؛ در صورت تکرار توی یک ثانیه نام و ایمیل جدید بده. متغیر `movieId` برای جزئیات و صندلی‌ها استفاده می‌شه؛ بدنه خرید مستقل هست و `movieId` و `seatNumber` اون رو هم در صورت نیاز تغییر بده. شناسه‌ها توی دیتابیس‌های غیربکر ممکنه با نمونه‌ها فرق داشته باشن.

## نمونه درخواست و پاسخ API

مسیرها به آدرس پایه اضافه می‌شن. درخواست‌های تغییر داده به هدر `X-XSRF-TOKEN` و کوکی CSRF نیاز دارن؛ عملیات محافظت‌شده هم به کوکی نشست نیاز دارن. زمان‌ها و شناسه‌ها نمونه‌ان. قالب پاسخ موفق:

```json
{"timestamp":"2026-10-30T10:00:00","status":200,"message":"Current user","data":{"id":2,"username":"user1","email":"user1@example.com","role":"USER"}}
```

توی جدول زیر فقط مقدار نمونه `data` نمایش داده شده؛ `status` و `message` هم توی پوشش پاسخ قرار دارن. خروجی فهرست‌ها و صندلی‌ها برای کوتاه‌تر شدن به شکل توضیحی نمایش داده شده.

| روش و مسیر | بدنه یا پارامتر نمونه | وضعیت و نمونه data |
|---|---|---|
| GET /api/auth/csrf | — | 200؛ `{"headerName":"X-XSRF-TOKEN","token":"..."}` |
| POST /api/auth/register | `{"username":"newuser","email":"newuser@example.com","password":"password123"}` | 201؛ `{"id":3,"username":"newuser","email":"newuser@example.com","role":"USER"}` |
| POST /api/auth/login | `{"username":"user1","password":"user123"}` | 200؛ شیء کاربر مانند نمونه بالا |
| GET /api/auth/me | — | 200؛ شیء کاربر جاری مانند نمونه بالا |
| POST /api/auth/logout | — | 200؛ `null` |
| GET /api/movies | — | 200؛ آرایه اشیای فیلم؛ نمونه فیلم توی ادامه |
| GET /api/movies/search?title=Interstellar | `title=Interstellar` | 200؛ آرایه فیلم‌های مطابق عنوان |
| GET /api/movies/1 | — | 200؛ شیء فیلم زیر |
| GET /api/movies/1/available-seats | — | 200؛ `{"movieId":1,"availableSeats":[2,3,...,50]}`؛ صندلی ۱ توی داده اولیه فروخته شده |
| POST /api/admin/movies | بدنه فیلم توی بخش APIها | 201؛ شیء فیلم ساخته‌شده شامل `id` و `cinemaHall` |
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

نمونه `data` برای بلیط خریداری‌شده و آمار فروش بر اساس داده اولیه، قبل از سایر خریدها؛ درآمد به تومنه:

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

</div>
