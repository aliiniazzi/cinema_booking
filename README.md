<div dir="rtl" align="right">

# سامانه رزرو بلیط سینما

این پروژه یه بک‌اند برای رزرو بلیط سینماست. کاربر می‌تونه فیلم‌ها و صندلی‌های خالی رو ببینه، بلیط بخره و در صورت نیاز لغوش کنه. مدیر هم می‌تونه فیلم اضافه کنه، اطلاعاتش رو تغییر بده یا حذفش کنه و آمار فروش رو ببینه.

پروژه با Java 25 و Spring Boot نوشته شده و از Maven و MySQL استفاده می‌کنه. ورود کاربران با Session انجام می‌شه و رمزها با BCrypt ذخیره می‌شن.

## قبل از شروع

برای اجرای پروژه دو راه داری: مستقیم روی سیستم خودت یا با Docker. برای اجرای مستقیم به Java 25، Maven و MySQL نیاز داری. برای اجرای Docker، نصب و روشن بودن Docker Desktop کافیه؛ MySQL هم داخل Docker بالا میاد.

اول یه ترمینال باز کن و برو توی پوشه‌ای که فایل `pom.xml` داخلشه. همه دستورهای پایین رو باید از همون پوشه اجرا کنی.

## اجرا بدون Docker

### ۱. دیتابیس رو آماده کن

سرویس MySQL رو روشن کن. بعد با MySQL Workbench یا خط فرمان به MySQL وصل شو و اگه دیتابیس `cinema_booking` رو نداری، این دستور رو اجرا کن:

<div dir="ltr" align="left">

```sql
CREATE DATABASE cinema_booking CHARACTER SET utf8mb4;
```

</div>

توی فایل `src/main/resources/application.properties` آدرس، نام کاربری و رمز MySQL تنظیم شده. اگه تنظیمات سیستم خودت فرق می‌کنه، مقادیر `spring.datasource.url`، `spring.datasource.username` و `spring.datasource.password` رو تغییر بده. برنامه بعد از اجرا جدول‌ها رو خودش می‌سازه.

### ۲. برنامه رو اجرا کن

توی PowerShell داخل پوشه پروژه، مسیر JDK رو تنظیم کن و برنامه رو اجرا کن:

<div dir="ltr" align="left">

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.1'
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" spring-boot:run
```

</div>

اگه Java جای دیگه‌ای نصب شده، مسیر خط اول رو با مسیر JDK خودت عوض کن. اگه تنظیمات Maven روی سیستمت مشکلی نداره، می‌تونی فقط `mvn spring-boot:run` رو اجرا کنی.

اگه با IntelliJ کار می‌کنی، Project SDK و Maven Runner JDK رو روی Java 25 بذار و کلاس `CinemaBookingApplication` رو اجرا کن.

### ۳. آدرس برنامه رو باز کن

بعد از اینکه برنامه بالا اومد، Swagger رو باز کن:

<div dir="ltr" align="left">

```text
http://localhost:8080/swagger-ui/index.html
```

</div>

اگه پورت 8080 اشغال بود، برنامه رو با پورت دیگه‌ای اجرا کن:

<div dir="ltr" align="left">

```powershell
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"
```

</div>

توی این حالت، به جای 8080 از 18080 توی آدرس Swagger استفاده کن.

## اجرا با Docker Compose

### ۱. Docker Desktop رو روشن کن

Docker Desktop و Compose باید نصب باشن. Docker Desktop رو باز کن و صبر کن تا آماده بشه. توی این روش لازم نیست MySQL ویندوز رو روشن کنی؛ یه MySQL جدا داخل Docker ساخته می‌شه.

### ۲. کانتینرها رو بالا بیار

توی PowerShell، داخل پوشه پروژه، این دستور رو بزن:

<div dir="ltr" align="left">

```powershell
docker compose up -d --build
```

</div>

دفعه اول دانلود imageها و وابستگی‌های پروژه ممکنه چند دقیقه طول بکشه. برای دیدن وضعیت و لاگ برنامه:

<div dir="ltr" align="left">

```powershell
docker compose ps
docker compose logs -f app
```

</div>

با `Ctrl+C` فقط از نمایش لاگ خارج می‌شی و کانتینرها روشن می‌مونن. بعد از بالا اومدن برنامه، Swagger اینجاست:

<div dir="ltr" align="left">

```text
http://localhost:8082/swagger-ui/index.html
```

</div>

اگه پورت 8082 اشغال بود، قبل از اجرای Compose پورت دیگه‌ای انتخاب کن:

<div dir="ltr" align="left">

```powershell
$env:APP_PORT = '8083'
docker compose up -d --build
```

</div>

حالا Swagger رو با پورت 8083 باز کن. اگه برنامه بالا نیومد، خروجی `docker compose ps` و `docker compose logs db app` رو ببین؛ برنامه منتظر آماده‌شدن MySQL می‌مونه.

### ۳. خاموش‌کردن برنامه

<div dir="ltr" align="left">

```powershell
docker compose down
```

</div>

این دستور کانتینرها رو خاموش می‌کنه، ولی داده‌های MySQL داخل volume باقی می‌مونن. اگه عمداً بخوای همه داده‌های دیتابیس Docker رو پاک کنی و از اول شروع کنی، دستور `docker compose down -v` رو بزن.

رمزهای پیش‌فرض Docker فقط برای محیط توسعه‌ان. قبل از اولین اجرا می‌تونی با متغیرهای محیطی `MYSQL_PASSWORD` و `MYSQL_ROOT_PASSWORD` عوضشون کنی. تغییر این متغیرها بعد از ساخته‌شدن volume، رمز دیتابیس موجود رو تغییر نمی‌ده. تنظیمات Docker توی `docker/application.properties` هست و فایل تنظیمات محلی همراه رمز MySQL وارد image نمی‌شه.

## داده‌های اولیه

وقتی هر چهار جدول پروژه خالی باشن، برنامه به‌طور خودکار این داده‌ها رو اضافه می‌کنه:

- مدیر: نام کاربری `admin` و رمز `admin123`
- کاربر عادی: نام کاربری `user1` و رمز `user123`
- یه سالن با ۵۰ صندلی و سه فیلم نمونه
- یه بلیط فعال برای صندلی ۱ فیلم اول

اگه دیتابیس از قبل داده داشته باشه، Seeder دوباره اجرا نمی‌شه و اطلاعات حذف‌شده یا ویرایش‌شده رو برنمی‌گردونه. قبل از اینکه پروژه رو جایی به اشتراک بذاری، رمز MySQL محلی رو از فایل تنظیمات بردار.

## تست با Swagger

بعد از اجرای برنامه، آدرس Swagger رو با پورت همون روش اجرا باز کن: برای اجرای معمولی 8080 و برای Docker به‌صورت پیش‌فرض 8082.

برای تست با حساب کاربر، این مراحل رو به ترتیب انجام بده:

1. توی بخش Authentication، درخواست `GET /api/auth/csrf` رو باز کن و دکمه‌های **Try it out** و **Execute** رو بزن.
2. درخواست `POST /api/auth/login` رو باز کن و با نام کاربری `user1` و رمز `user123` وارد شو.
3. دوباره `GET /api/auth/csrf` رو اجرا کن. توکن قبلی بعد از ورود عوض می‌شه.
4. حالا می‌تونی فیلم‌ها و صندلی‌های آزاد رو ببینی، بلیط بخری و بلیط خودت رو لغو کنی.

مرورگر کوکی نشست و CSRF رو نگه می‌داره. برای تست بخش مدیر با `admin` و `admin123` وارد شو. بعد از ورود یا خروج دوباره توکن CSRF بگیر. ثبت‌نام و ورود هم به توکن نیاز دارن.

## تست با Postman

فایل زیر رو با گزینه **Import** وارد Postman کن:

<div dir="ltr" align="left">

```text
postman/Cinema-Booking.postman_collection.json
```

</div>

آدرس پیش‌فرض کالکشن برای Docker تنظیم شده. اگه برنامه رو مستقیم اجرا کردی، توی متغیرهای کالکشن مقدار `baseUrl` رو به `http://localhost:8080` یا پورت خودت تغییر بده. کوکی‌های Postman باید فعال باشن و همه درخواست‌ها با یه hostname فرستاده بشن.

برای تست خرید:

1. **Get CSRF token** رو اجرا کن تا توکن توی متغیر `csrfToken` ذخیره بشه.
2. **Login USER** رو بزن و بعد دوباره **Get CSRF token** رو اجرا کن.
3. صندلی‌های آزاد رو ببین و **Book ticket** رو بزن. شناسه بلیط توی `ticketId` ذخیره می‌شه. اگه صندلی ۱۲ آزاد نبود، شماره یه صندلی آزاد رو توی بدنه درخواست بذار.
4. دوباره همون صندلی رو بخر؛ باید خطای 409 بگیری. بعد با **Cancel ticket** بلیط رو لغو کن تا صندلی آزاد بشه.

برای تست مدیر، به ترتیب **Logout**، **Get CSRF token**، **Login ADMIN** و دوباره **Get CSRF token** رو اجرا کن. قبل از ویرایش یا حذف فیلم، با **Create movie** یه فیلم بساز تا شناسه‌اش توی `adminMovieId` ذخیره بشه.

متغیر `movieId` کالکشن برای درخواست جزئیات فیلم و صندلی‌هاست. بدنه درخواست خرید جداست؛ اگه فیلم رو عوض کردی، شناسه داخل بدنه خرید رو هم تغییر بده. درخواست ثبت‌نام با زمان جاری نام و ایمیل می‌سازه؛ اگه دو بار توی یک ثانیه اجراش کردی، ممکنه لازم باشه نام و ایمیل جدید بذاری.

## لیست APIها

مسیرها رو به آدرس برنامه اضافه کن. درخواست‌های تغییر اطلاعات به هدر `X-XSRF-TOKEN` و کوکی CSRF نیاز دارن؛ APIهای محافظت‌شده هم به کوکی نشست نیاز دارن.

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

نمونه بدنه ساخت یا ویرایش فیلم؛ شناسه سالن باید توی دیتابیس وجود داشته باشه و قیمت به تومنه:

<div dir="ltr" align="left">

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

</div>

نمونه بدنه خرید بلیط:

<div dir="ltr" align="left">

```json
{"movieId": 1, "seatNumber": 12}
```

</div>

## نمونه درخواست و پاسخ API

مسیرها به آدرس پایه اضافه می‌شن. درخواست‌های تغییر داده به هدر `X-XSRF-TOKEN` و کوکی CSRF نیاز دارن؛ عملیات محافظت‌شده هم به کوکی نشست نیاز دارن. زمان‌ها و شناسه‌ها نمونه‌ان. قالب پاسخ موفق:

<div dir="ltr" align="left">

```json
{"timestamp":"2026-10-30T10:00:00","status":200,"message":"Current user","data":{"id":2,"username":"user1","email":"user1@example.com","role":"USER"}}
```

</div>

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

<div dir="ltr" align="left">

```json
{"id":1,"title":"The Shawshank Redemption","genre":"Drama","durationMinutes":142,"releaseDate":"1994-09-23","description":"A story of hope and friendship in prison.","price":120000,"cinemaHall":{"id":1,"name":"Hall 1","capacity":50}}
```

</div>

نمونه `data` برای بلیط خریداری‌شده و آمار فروش بر اساس داده اولیه، قبل از سایر خریدها؛ درآمد به تومنه:

<div dir="ltr" align="left">

```json
{"id":2,"movieId":1,"movieTitle":"The Shawshank Redemption","seatNumber":12,"purchaseDate":"2026-10-30T10:00:00","price":120000,"status":"ACTIVE","cancelledAt":null}
```

</div>

<div dir="ltr" align="left">

```json
{"totalTickets":1,"totalRevenue":120000,"movies":[{"movieId":1,"movieTitle":"The Shawshank Redemption","activeTicketCount":1,"totalRevenue":120000},{"movieId":2,"movieTitle":"Interstellar","activeTicketCount":0,"totalRevenue":0},{"movieId":3,"movieTitle":"Inception","activeTicketCount":0,"totalRevenue":0}]}
```

</div>

نمونه خطای خرید مجدد صندلی:

<div dir="ltr" align="left">

```json
{"timestamp":"2026-10-30T10:00:00","status":409,"error":"CONFLICT","message":"صندلی شماره 12 قبلاً فروخته شده است","path":"/api/tickets/book"}
```

</div>


## اجرای تست‌ها

برای اجرای همه تست‌ها، از داخل پوشه پروژه این دستور رو بزن:

<div dir="ltr" align="left">

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.1'
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" test
```

</div>

برای اجرای فقط تست همزمانی:

<div dir="ltr" align="left">

```powershell
mvn -s "$env:USERPROFILE\.m2\settings.xml" "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" "-Dtest=TicketConcurrencyTest" test
```

</div>

تست‌ها به‌صورت پیش‌فرض از H2 حافظه‌ای استفاده می‌کنن و داده‌های MySQL رو تغییر نمی‌دن. برای اجرای تست روی MySQL، از یه اسکیمای آزمایشی جدا استفاده کن؛ چون تنظیم تست `create-drop` هست و نباید به دیتابیس اصلی وصل بشه.

تست همزمانی با `ExecutorService` دو تراکنش برای خرید یه صندلی اجرا می‌کنه. با `CyclicBarrier` هر دو اول صندلی رو آزاد می‌بینن. تست چک می‌کنه یکی از خریدها موفق بشه، دومی خطای صندلی فروخته‌شده بگیره و فقط یه بلیط فعال توی دیتابیس بمونه. علاوه بر این، دیتابیس باید درج مستقیم بلیط تکراری رو هم رد کنه. ممکنه خطای SQL همین تلاش عمدی توی لاگ دیده بشه؛ نتیجه نهایی تست نشون می‌ده بررسی موفق بوده یا نه.

## چند نکته درباره پروژه

- قیمت هر فیلم به تومن توی خود فیلم ذخیره می‌شه و بلیط، قیمت زمان خرید رو نگه می‌داره.
- لغو بلیط صندلی رو آزاد می‌کنه، ولی سابقه بلیط باقی می‌مونه. آمار فقط بلیط‌های فعال رو حساب می‌کنه و سابقه فروش فیلم‌های حذف‌شده رو هم نگه می‌داره.
- محدودیت یکتای دیتابیس روی فیلم و صندلی فعال جلوی فروش تکراری رو می‌گیره. ستون محاسبه‌شده `active_seat_number` این محدودیت رو برای بلیط‌های فعال اعمال می‌کنه. برنامه موقع شروع وجود این محدودیت رو چک می‌کنه. `@Version` و قفل خوش‌بینانه هم تغییر همزمان فیلم رو کنترل می‌کنن.
- موجودیت‌ها از `BaseEntity` ارث می‌برن. زمان و نام کاربر ایجادکننده و ویرایش‌کننده با JPA Auditing ثبت می‌شن و برای عملیات بدون کاربر، نام `system` ذخیره می‌شه. `@SQLRestriction` ردیف‌های حذف‌شده رو از پرس‌وجوهای Entity مخفی می‌کنه. حذف منطقی با `markDeleted()` و ذخیره موجودیت انجام می‌شه؛ `repository.delete` حذف فیزیکیه. لغو بلیط از حذف منطقی جداست.
- خطاها و پیام‌های اعتبارسنجی فارسی هستن و locale برنامه روی `fa` تنظیم شده.

## استفاده از Functional Programming

- `BookingRule` یه Functional Interface برای قوانین رزروه. دو Lambda ظرفیت سالن و آزادبودن صندلی رو بررسی می‌کنن و قوانین با `forEach` اجرا می‌شن.
- توی `MovieService.list` از `Predicate<Movie>` برای فیلتر فیلم‌ها استفاده شده تا شرط جستجو از مرتب‌سازی جدا باشه.
- توی همون متد، `Comparator` و Lambda فیلم‌ها رو بر اساس عنوان مرتب می‌کنن.
- از Stream و Method Reference برای تبدیل و فیلتر فیلم‌ها، پیداکردن صندلی‌های آزاد و جمع تعداد و درآمد فروش استفاده شده.

</div>
