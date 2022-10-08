package scc.data.JSON;

import java.util.Date;

public record AuctionJSON(String title,
                          String description,
                          String userId,
                          Date endTime,
                          long minimumPrice,
                          String imageBase64){}
