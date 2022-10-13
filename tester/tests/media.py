import recon
import scc


def group(endpoints: scc.Endpoints):
    return recon.TestGroup(
        [
            upload_image(endpoints),
            upload_image_and_verify(endpoints),
            download_missing_image(endpoints),
        ]
    )


@recon.test_case("media", "upload image")
def upload_image(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    response = rclient.upload_media(scc.random_image())
    with recon.validate(response) as validator:
        validator.status_code(200)


@recon.test_case("media", "upload image and verify download")
def upload_image_and_verify(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    client = scc.Client(endpoints)

    image = scc.random_image()
    media_id = client.upload_media(image)
    response = rclient.download_media(media_id)
    with recon.validate(response) as validator:
        validator.status_code(200)
        validator.content_type("application/octet-stream")
        validator.content(image)


@recon.test_case("media", "download missing image")
def download_missing_image(endpoints: scc.Endpoints):
    rclient = scc.RawClient(endpoints)
    response = rclient.download_media("missing")
    with recon.validate(response) as validator:
        validator.status_code(404)
