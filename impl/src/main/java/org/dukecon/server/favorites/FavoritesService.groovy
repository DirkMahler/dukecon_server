package org.dukecon.server.favorites

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.dukecon.model.Conference
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.time.ZoneOffset

/**
 * @author Falk Sippach, falk@jug-da.de, @sippsack
 */
@Service
@TypeChecked
@Slf4j
class FavoritesService {
    private FavoritesRepository favoritesRepository

    @Inject
    FavoritesService(FavoritesRepository favoritesRepository) {
        this.favoritesRepository = favoritesRepository
    }

    /**
     * Returns a list of favorites per event with meta data (title, speaker, location, locationCapacity and start time.
     *
     * @param conference for which event favorites should be returned
     * @return List of EventFavorites
     */
    List<EventFavorites> getAllFavoritesForConference(Conference conference) {
        def eventIds = conference.events.id
        def events = favoritesRepository.getAllFavoritesPerEvent(eventIds)

        return conference.events.collect { event ->
            EventFavorites e = events.find { it.eventId == event.id } ?: new EventFavorites(event.id, 0L)
            e.title = event?.title
            e.speakers = event?.speakers?.name?.join(', ')
            e.location = event?.location?.names['de']
            e.locationCapacity = event?.location?.capacity
            e.start = Date.from(event.start.toInstant(ZoneOffset.UTC))
            e.type = event?.type?.names?.get('de') ?: ''
            e.track = event?.track?.names?.get('de') ?: ''
            return e
        }.sort { e1, e2 -> e1.start <=> e2.start ?: e1.type <=> e2.type ?: e2.numberOfFavorites <=> e1.numberOfFavorites ?: e1.title <=> e2.title }
    }
}
