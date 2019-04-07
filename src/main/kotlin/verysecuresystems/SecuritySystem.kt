package verysecuresystems

import org.http4k.core.Events
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.HandleUpstreamRequestFailed
import org.http4k.filter.ServerFilters
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import verysecuresystems.api.Api
import verysecuresystems.diagnostic.Auditor
import verysecuresystems.diagnostic.Diagnostic
import verysecuresystems.external.EntryLogger
import verysecuresystems.external.UserDirectory
import verysecuresystems.web.Web
import java.time.Clock

/**
 * Sets up the business-level API for the application. Note that the generic clients on the constructor allow us to
 * inject non-HTTP versions of the downstream dependencies so we can run tests without starting up real HTTP servers.
 */
object SecuritySystem {

    operator fun invoke(clock: Clock, events: Events,
                        userDirectoryHttp: HttpHandler,
                        entryLoggerHttp: HttpHandler): HttpHandler {
        val userDirectory = UserDirectory(userDirectoryHttp)
        val entryLogger = EntryLogger(entryLoggerHttp, clock)
        val inhabitants = Inhabitants()

        val app = routes(
            "/api" bind Api(userDirectory, entryLogger, inhabitants),
            "/internal" bind Diagnostic(clock),
            Web(userDirectory),
            "/" bind static(Classpath("public"))
        )

        return Auditor(clock, events)
            .then(ServerFilters.CatchAll())
            .then(ServerFilters.CatchLensFailure)
            .then(ServerFilters.HandleUpstreamRequestFailed())
            .then(app)
    }
}


