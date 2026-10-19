from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView

from accounts import views

urlpatterns = [
    path("health", views.health),
    path("auth/signup", views.signup),
    path("auth/login", views.LoginView.as_view()),
    path("auth/refresh", TokenRefreshView.as_view()),
    path("auth/me", views.me),
]
