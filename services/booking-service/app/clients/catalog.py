import httpx

from app.config import settings


class CatalogUnavailable(Exception):
    pass


client = httpx.Client(base_url=settings.catalog_url, timeout=3.0)


def _get(path):
    try:
        resp = client.get(path)
    except httpx.HTTPError:
        raise CatalogUnavailable()
    if resp.status_code == 404:
        return None
    if resp.status_code != 200:
        raise CatalogUnavailable()
    return resp.json()


def get_show(show_id):
    return _get(f"/shows/{show_id}")


def get_seats(show_id):
    return _get(f"/shows/{show_id}/seats")
