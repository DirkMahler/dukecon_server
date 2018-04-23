package org.dukecon.server.repositories.apache

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.text.WordUtils
import org.dukecon.model.*
import org.dukecon.server.conference.ConferencesConfiguration
import org.dukecon.server.conference.SpeakerImageService
import org.dukecon.server.favorites.PreferencesService
import org.dukecon.server.repositories.ConferenceDataExtractor
import org.dukecon.server.repositories.RawDataMapper
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * @author Christofer Dutz, christofer.dutz@codecentric.de, @ChristoferDutz
 */
@Slf4j
class ApacheDataExtractor implements ConferenceDataExtractor, ApplicationContextAware {

    private SpeakerImageService speakerImageService
    private PreferencesService preferencesService

    private final RawDataMapper rawDataMapper
    def conferenceJson
    private final String conferenceId
    private final LocalDate startDate
    private final String conferenceUrl
    private final String conferenceHomeUrl
    private final String conferenceName

    ApacheDataExtractor(ConferencesConfiguration.Conference config, RawDataMapper rawDataMapper, SpeakerImageService speakerImageService) {
        log.debug ("Extracting data for '{}'", config)
        this.conferenceId = config.id
        this.rawDataMapper = rawDataMapper
        this.startDate = config.startDate
        this.conferenceName = config.name
        this.conferenceUrl = config.url
        this.conferenceHomeUrl = config.homeUrl
        this.speakerImageService = speakerImageService
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.preferencesService = applicationContext.getBean(PreferencesService)
    }

    @Override
    Conference getConference() {
        return buildConference()
    }

    @Override
    RawDataMapper getRawDataMapper() {
        return this.rawDataMapper
    }

    Conference buildConference() {
        log.debug("Building conference '{}' (name: {}, url: {})", conferenceId, conferenceName, conferenceUrl)
        this.rawDataMapper.initMapper()
        this.conferenceJson = this.rawDataMapper.asMap().eventsData.rooms
        ParseContext ctx = new ParseContext()
        ctx.languages.put("en", new Language("en", "en", 1, [en: "English"], null))
        parseRooms(ctx, conferenceJson)

        MetaData metaData = MetaData.builder()
                .languages(new ArrayList<Speaker>(ctx.languages.values()))
                .eventTypes(new ArrayList<Speaker>(ctx.eventTypes.values()))
                .tracks(new ArrayList<Speaker>(ctx.tracks.values()))
                .locations(new ArrayList<Speaker>(ctx.locations.values()))
                .audiences(new ArrayList<Speaker>(ctx.audiences.values()))
                .build()
        Conference conference = Conference.builder().id(conferenceId)
                .name(conferenceName)
                .url(conferenceUrl)
                .homeUrl(conferenceHomeUrl)
                .events(new ArrayList<Event>(ctx.events.values()))
                .speakers(new ArrayList<Speaker>(ctx.speakers.values()))
                .metaData(metaData)
                .build()
        return conference
    }

    private static void parseRooms(ParseContext ctx, def json) {
        List<Map> rooms = (List<Map>) json
        for (Map room : rooms) {
            String roomName = room.get("name")
            List<Map> days = (List<Map>) room.get("days")
            for(Map day : days) {
                parseDay(ctx, roomName, day)
            }
        }
    }

    private static void parseDay(ParseContext ctx, String roomName, def json) {
        if(json.slots) {
            List<Map> slots = json.slots
            for (Map slot : slots) {
                parseSlot(ctx, roomName, slot)
            }
        }
    }

    private static void parseSlot(ParseContext ctx, String roomName, def json) {
        if(json.talk) {
            String speakerName = json.talk.speaker
            String[] speakers = speakerName.split(",")
            List<Speaker> curTalksSpeakers = new LinkedList<>()
            for(String speakerString : speakers) {
                speakerString = speakerString.trim()
                if (!ctx.speakers.containsKey(speakerString)) {
                    String firstName
                    String lastName
                    if(speakerString.contains(" ")) {
                        firstName = WordUtils.capitalizeFully(speakerString.split(" ")[0].trim())
                        lastName = WordUtils.capitalizeFully(speakerString.substring(firstName.length()).trim())
                    } else {
                        firstName = WordUtils.capitalizeFully(speakerString)
                        lastName = ""
                    }
                    Speaker speaker = Speaker.builder()
                            .id(speakerString)
                            .name(speakerString)
                            .firstname(firstName)
                            .lastname(lastName)
                            .bio((String) json.talk.bio)
                            .build()
                    ctx.speakers.put(speakerString, speaker)
                    curTalksSpeakers.add(speaker)
                }
            }
            if (!ctx.locations.containsKey(roomName)) {
                Location location = Location.builder()
                        .id(roomName)
                        .order(1)
                        .names([en: roomName])
                        // TODO: Dummy for now ...
                        .capacity(100)
                        .build()
                ctx.locations.put(roomName, location)
            }
            String trackName = json.talk.category
            if (!ctx.tracks.containsKey(trackName)) {
                Track track = Track.builder()
                        .id(trackName)
                        .order(1)
                        .names([en: trackName])
                        .build()
                ctx.tracks.put(trackName, track)
            }
            String eventType = json.talk.ttype
            if (!ctx.eventTypes.containsKey(trackName)) {
                EventType type = EventType.builder()
                        .id(eventType)
                        .order(1)
                        .names([en: eventType])
                        .build()
                ctx.eventTypes.put(eventType, type)
            }
            if(!ctx.audiences.containsKey("dev")) {
                Audience audience = Audience.builder()
                        .id("dev")
                        .order(1)
                        .names([en: "Devlopers"])
                        .build()
                ctx.audiences.put("dev", audience);
            }

            String eventId = json.talk.id
            Event event = Event.builder()
                    .id(eventId)
                    .track(ctx.tracks.get(trackName))
                    .type(ctx.eventTypes.get(eventType))
                    .location(ctx.locations.get(roomName))
                    .start(LocalDateTime.ofInstant(
                        new Date(Long.valueOf((String) json.starttime) * 1000).toInstant(), ZoneId.systemDefault()))
                    .end(LocalDateTime.ofInstant(
                        new Date(Long.valueOf((String) json.endtime) * 1000).toInstant(), ZoneId.systemDefault()))
                    .speakers(curTalksSpeakers)
                    .language(ctx.languages.get("en"))
                    .audience(ctx.audiences.get("dev"))
                    .title((String) json.talk.title)
                    .abstractText((String) json.talk.description)
                    .documents(new HashMap<String, String>())
                    .build()
            ctx.events.put(eventId, event)
        }
    }

    private static class ParseContext {
        private Map<String, Audience> audiences = new HashMap<>()
        private Map<String, Event> events = new HashMap<>()
        private Map<String, EventType> eventTypes = new HashMap<>()
        private Map<String, Location> locations = new HashMap<>()
        private Map<String, Speaker> speakers = new HashMap<>()
        private Map<String, Track> tracks = new HashMap<>()
        private Map<String, Language> languages = new HashMap<>()
    }

}
