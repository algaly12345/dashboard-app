# تعليمات الدمج

## ١) ملفات جديدة كاملة (انسخها بنفس المسار الموجود بهذا الأرشيف)
- src/main/java/com/realestate/admin/controller/web/EstateCreateController.java
- src/main/java/com/realestate/admin/service/NhcService.java
- src/main/java/com/realestate/admin/controller/web/PublicPageController.java
- src/main/resources/templates/estate-new.html
- src/main/resources/templates/estate-preview.html
- src/main/resources/templates/public/privacy-policy.html
- src/main/resources/templates/public/terms.html

## ٢) تعديلات يدوية بملفات موجودة أصلاً عندك (مو موجودة بهذا الأرشيف)

### AppUserRepository.java — أضف
```java
import java.util.Optional;

Optional<AppUser> findByPhone(String phone);

@org.springframework.data.jpa.repository.Query("select coalesce(max(u.id), 0) from AppUser u")
Long findMaxId();
```

### SecurityConfig.java — بسطر permitAll الحالي أضف
```
"/privacy-policy", "/terms-and-conditions"
```

### SettingsController.java — بدالة GET أضف
```java
model.addAttribute("nhcClientId", settingsService.get("nhc_client_id", ""));
model.addAttribute("nhcHasSecret", !settingsService.get("nhc_client_secret", "").isBlank());
```

### SettingsController.java — بدالة POST (save) أضف
```java
settingsService.set("nhc_client_id", form.get("nhcClientId"));
if (form.get("nhcClientSecret") != null && !form.get("nhcClientSecret").isBlank()) {
    settingsService.set("nhc_client_secret", form.get("nhcClientSecret"));
}
```

### settings.html — أضف قسم nhc-settings-section.html (بنفس هذا الأرشيف) بعد قسم R2

### estates.html — أضف زر بأعلى القائمة
```html
<a th:href="@{/estates/new}" class="btn btn-gold">
    <i class="bi bi-plus-lg"></i> إضافة عقار (NHC)
</a>
```

### messages.properties / messages_ar.properties — أضف
```
estate.newTitle=Add Property (NHC)  /  إضافة عقار (NHC)
estate.previewTitle=Review Property Data  /  معاينة بيانات العقار
settings.section.nhc=NHC Integration  /  تكامل هيئة العقار (NHC)
settings.nhcCredentials=Credentials  /  بيانات الاعتماد
settings.nhcHint=... /  ...
settings.nhcClientId=Client ID  /  معرّف العميل
settings.nhcClientSecret=Client Secret  /  المفتاح السري
```

## ٣) بعد الدمج
```bash
mvn spring-boot:run   # تأكد يبني بدون أخطاء محليًا أول
git add -A
git commit -m "feat: NHC estate creation + public legal pages"
git push origin main
```
انتظر GitHub Actions ✅ ثم:
```bash
sudo kubectl rollout restart deployment/dashboard-app -n dashboard
```
