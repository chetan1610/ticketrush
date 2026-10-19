from django.contrib import admin

from accounts.models import User


@admin.register(User)
class UserAdmin(admin.ModelAdmin):
    list_display = ["email", "name", "is_active", "is_staff", "created_at"]
    list_filter = ["is_active", "is_staff"]
    search_fields = ["email", "name"]
    # Passwords are set via signup, createsuperuser or changepassword, never edited here.
    exclude = ["password"]
    readonly_fields = ["last_login", "created_at"]
