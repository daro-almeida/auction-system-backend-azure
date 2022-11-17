class Endpoints:
    base: str
    user: str
    user_auth: str
    media: str
    auction: str

    def __init__(self, base="http://localhost:8080"):
        self.base = base
        self.user = base + "/rest/user"
        self.user_auth = base + "/rest/user/auth"
        self.media = base + "/rest/media"
        self.auction = base + "/rest/auction"
