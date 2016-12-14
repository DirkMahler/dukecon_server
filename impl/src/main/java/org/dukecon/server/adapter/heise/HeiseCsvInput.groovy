package org.dukecon.server.adapter.heise

import com.xlson.groovycsv.PropertyMapper
import org.dukecon.server.adapter.RawDataMapper
import org.dukecon.server.adapter.RawDataResourceSupplier

import static com.xlson.groovycsv.CsvParser.parseCsv

/**
 * @author Falk Sippach, falk@jug-da.de, @sippsack
 */
class HeiseCsvInput implements Iterable<PropertyMapper>, RawDataMapper {

    private List<PropertyMapper> input

    HeiseCsvInput(String filename) {
        this(HeiseCsvInput.class.getResourceAsStream("/${filename}"))
    }

    HeiseCsvInput(InputStream inputStream) {
        this.input = tidyUpCsvIterator(parseCsv(new InputStreamReader(inputStream, "UTF-8"), separator: ';'))
    }

    /**
     * Removes empty lines in CSV.
     *
     * @param input
     * @return
     */
    private List<PropertyMapper> tidyUpCsvIterator(input) {
        input.findAll { PropertyMapper row -> !row.values[0].isEmpty() }
    }

    @Override
    Iterator<PropertyMapper> iterator() {
        return input.iterator()
    }

    @Override
    Map<String, Object> asMap() {
        return [events:input]
    }

    @Override
    void useBackup(RawDataResourceSupplier resourceSupplier) {
        // TODO implement
    }
}
