package org.dukecon.server.conference

/**
 * Created by ascheman on 17.06.17.
 */
interface SpeakerImageService {
    static class ImageWithName {
        final String filename
        final byte[] content

        ImageWithName(String filename, byte[] content) {
            this.filename = filename
            this.content = content
        }
    }

    String addImage(byte[] content, String filename)
    String addImage(String contentBase64, String filename)
    ImageWithName getImage(String md5Hash)
}