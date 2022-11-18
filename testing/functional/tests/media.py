import recon
import scc
from scc.clients import Client, RawClient
from scc.endpoints import Endpoints
from . import test_case


@test_case("media/upload image")
def upload_image(endpoints: Endpoints):
    rclient = RawClient(endpoints)
    response = rclient.upload_media(scc.random_image())
    with recon.validate(response) as validator:
        validator.status_code(200)


@test_case("media/upload image and verify download")
def upload_image_and_verify(endpoints: Endpoints):
    client = Client(endpoints)

    image = scc.random_image()
    media_id = client.upload_media(image)
    response = client.raw.download_media(media_id)
    with recon.validate(response) as validator:
        validator.status_code(200)
        validator.content_type("application/octet-stream")
        validator.content(image)


@test_case("media/download missing image")
def download_missing_image(endpoints: Endpoints):
    rclient = RawClient(endpoints)
    response = rclient.download_media("missing")
    with recon.validate(response) as validator:
        validator.status_code([400, 404])  # missing is an invalid media id
