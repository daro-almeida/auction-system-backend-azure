
import java.time.format.DateTimeParseException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;

public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof WebApplicationException)
            return ((WebApplicationException) ex).getResponse();
        if (ex instanceof NumberFormatException)
            return Response.status(Status.BAD_REQUEST).entity("Invalid number format").build();
        if (ex instanceof DateTimeParseException)
            return Response.status(Status.BAD_REQUEST).entity("Invalid date format").build();
        ex.printStackTrace();
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
}