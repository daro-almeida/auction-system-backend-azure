
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import scc.exception.*;

public class ServiceExceptionMapper implements ExceptionMapper<ServiceException> {
    @Override
    public Response toResponse(ServiceException ex) {
        ex.printStackTrace();
        System.out.println(ex.getMessage());

        if (ex instanceof AuctionNotFoundException)
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        if (ex instanceof BadCredentialsException)
            return Response.status(Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        if (ex instanceof BadRequestException)
            return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
        if (ex instanceof BidConflictException)
            return Response.status(Status.CONFLICT).entity(ex.getMessage()).build();
        if (ex instanceof BidNotFoundException)
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        if (ex instanceof InternalErrorException)
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        if (ex instanceof MediaNotFoundException)
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        if (ex instanceof QuestionAlreadyRepliedException)
            return Response.status(Status.CONFLICT).entity(ex.getMessage()).build();
        if (ex instanceof QuestionAlreadyRepliedException)
            return Response.status(Status.CONFLICT).entity(ex.getMessage()).build();
        if (ex instanceof QuestionNotFoundException)
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        if (ex instanceof UnauthorizedException)
            return Response.status(Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        if (ex instanceof UserAlreadyExistsException)
            return Response.status(Status.CONFLICT).entity(ex.getMessage()).build();
        if (ex instanceof UserNotFoundException)
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).build();
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
}