package org.dukecon.server.herbstcampus

import java.beans.Introspector

/**
 * @author Falk Sippach, falk@jug-da.de, @sippsack
 */
abstract class HerbstcampusAbstractEntityMapper<T> {
    List<T> entities

    HerbstcampusAbstractEntityMapper(input, String mappingName) {
        this.entities = input.collect { it[mappingName] }
                .unique()
                .findAll { it }
                .sort()
                .withIndex()
                .collect { value, index ->
                    owner.getEntityType().builder()
                    .id(index + 1 as String)
                    .icon("${getEntityName()}_${index + 1}.png")
                    .order(index + 1)
                    .names([de: calculateValue(value), en: calculateValue(value)])
                    .build()
        }
    }

    T entityForName(String name) {
        entities.find { calculateValue(name) == it.names.de }
    }

    protected String getEntityName() {
        Introspector.decapitalize(getEntityType().simpleName)
    }

    protected String calculateValue(String value) {
        value
    }

    Class<T> getEntityType() {
        getClass().getGenericSuperclass().getActualTypeArguments()[0]
    }
}
