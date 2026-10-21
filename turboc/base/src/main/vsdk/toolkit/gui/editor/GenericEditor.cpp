#include <stddef.h>
#include <string>

#include "java/lang/Double.h"
#include "java/lang/Float.h"
#include "java/lang/Integer.h"
#include "java/lang/Long.h"
#include "java/lang/NumberFormatException.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/Entity.h"
#include "vsdk/toolkit/common/EntityControlAccessor.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/gui/editor/ControlSpecification.h"
#include "vsdk/toolkit/gui/editor/GenericEditor.h"
#include "vsdk/toolkit/gui/editor/GenericEditorListener.h"


static bool typeIsSupported(const String &type)
{
    return type == "double" || type == "float" || type == "int" ||
        type == "long";
}

/**
@return the value of the text for a supported type, as a double
@throws java::NumberFormatException if the text is not a value of the type
*/
static double parseValue(const String &type, const String &text)
{
    if ( type == "double" ) {
        return Double::parseDouble(text);
    }
    if ( type == "float" ) {
        return Float::parseFloat(text);
    }
    if ( type == "int" ) {
        return Integer::parseInt(text);
    }
    if ( type == "long" ) {
        return (double)Long::parseLong(text);
    }
    throw NumberFormatException(String("Unsupported type ") + type);
}


GenericEditor::GenericEditor() :
    listener(NULL), entityDeleted(false), entity(NULL)
{
}

GenericEditor::~GenericEditor()
{
    detach();
    clearSpecifications();
}

void
GenericEditor::setListener(GenericEditorListener *listener)
{
    this->listener = listener;
}

void
GenericEditor::clearSpecifications()
{
    for ( long int i = 0; i < specifications.size(); i++ ) {
        delete specifications.get(i);
    }
    specifications.clear();
}

void
GenericEditor::build(Entity *entity)
{
    detach();
    this->entity = entity;
    clearSpecifications();

    if ( entity == NULL ) {
        Logger::reportMessage("GenericEditor", Logger::WARNING,
            "GenericEditor.build",
            "Null entity received, building an empty editor.");
        beginBuild("");
        showValidationMessage("No object to edit.");
        endBuild();
        return;
    }

    entity->addEntityListener(this);
    entityClassName = getSimpleClassName(*entity);
    beginBuild(entityClassName);

    ArrayList<String> &texts = entity->getControlSpecifications();
    for ( long int i = 0; i < texts.size(); i++ ) {
        const String &text = texts.get(i);
        ControlSpecification *specification = ControlSpecification::parse(text);
        if ( specification == NULL ) {
            continue;
        }
        if ( !typeIsSupported(specification->getType()) ) {
            Logger::reportMessage("GenericEditor", Logger::WARNING,
                "GenericEditor.build",
                String("Unsupported type \"") +
                specification->getType() +
                "\" in control specification \"" + text + "\" of class " +
                entityClassName + ", control skipped.");
            delete specification;
            continue;
        }
        String value;
        if ( !readValue(*specification, value) ) {
            delete specification;
            continue;
        }
        specifications.add(specification);
        addControl(specification, value);
    }

    if ( specifications.size() == 0 ) {
        showValidationMessage((String("No editable attributes for ") +
            entityClassName).c_str());
    }
    else {
        showValidationMessage(NULL);
    }
    endBuild();
}

void
GenericEditor::detach()
{
    if ( entity != NULL ) {
        entity->removeEntityListener(this);
    }
    entity = NULL;
    entityDeleted = false;
}

bool
GenericEditor::isEntityDeleted() const
{
    return entityDeleted;
}

void
GenericEditor::notifyEntityEvent(const EntityEvent &event)
{
    if ( event.getSource() != entity || entity == NULL ) {
        return;
    }
    if ( event.getType() == EntityEvent::DELETED ) {
        // Also emitted by the destructor of the entity, when only its
        // Entity part is alive: the class name is the one known at build
        String className = entityClassName;
        entity->removeEntityListener(this);
        entity = NULL;
        clearSpecifications();
        entityDeleted = true;
        clearControls(String("Entity deleted (") + className + ").");
    }
    else if ( event.getType() == EntityEvent::UPDATED ) {
        for ( long int i = 0; i < specifications.size(); i++ ) {
            String value;
            if ( readValue(*specifications.get(i), value) ) {
                setControlValue(specifications.get(i), value);
            }
        }
    }
}

Entity *
GenericEditor::getEntity() const
{
    return entity;
}

bool
GenericEditor::readValue(const ControlSpecification &specification,
                         String &outValue) const
{
    const EntityControlAccessor *getter = findGetter(specification);
    if ( getter == NULL ) {
        return false;
    }
    if ( !getter->readValue(*entity, outValue) ) {
        Logger::reportMessage("GenericEditor", Logger::WARNING,
            "GenericEditor.readValue",
            String("Cannot call get") + specification.getLabel() +
            "() on an object of class " + entityClassName + ".");
        return false;
    }
    return true;
}

bool
GenericEditor::updateValue(const ControlSpecification *specification,
                           const String &text)
{
    if ( entity == NULL || specification == NULL ) {
        return false;
    }

    std::string raw(text.c_str());
    size_t begin = raw.find_first_not_of(" \t\n\r\f\v");
    size_t end = raw.find_last_not_of(" \t\n\r\f\v");
    String trimmed = begin == std::string::npos ? String("") :
        String(raw.substr(begin, end - begin + 1).c_str());

    double numeric;
    try {
        numeric = parseValue(specification->getType(), trimmed);
    }
    catch ( const NumberFormatException & ) {
        showValidationMessage((specification->getLabel() + ": \"" + text +
            "\" is not a valid " + specification->getType() + ".").c_str());
        return false;
    }

    if ( !specification->contains(numeric) ) {
        showValidationMessage((specification->getLabel() + " must be in " +
            specification->getIntervalText() + ".").c_str());
        return false;
    }

    const EntityControlAccessor *setter = findSetter(*specification);
    if ( setter == NULL ) {
        showValidationMessage((specification->getLabel() +
            " cannot be changed.").c_str());
        return false;
    }
    if ( !setter->writeValue(*entity, trimmed) ) {
        Logger::reportMessage("GenericEditor", Logger::WARNING,
            "GenericEditor.updateValue",
            String("Cannot call set") + specification->getLabel() +
            " on an object of class " + entityClassName + ".");
        showValidationMessage((specification->getLabel() +
            " could not be changed.").c_str());
        return false;
    }

    showValidationMessage(NULL);
    Entity *changed = entity;
    changed->update();
    if ( listener != NULL ) {
        listener->notifyEntityChanged(changed);
    }
    return true;
}

const EntityControlAccessor *
GenericEditor::findGetter(const ControlSpecification &specification) const
{
    String methodName = String("get") + specification.getLabel();
    const EntityControlAccessor *accessor =
        entity->getControlAccessor(specification.getName());
    if ( accessor == NULL ) {
        reportMissingAccessor(specification, methodName + "()");
        return NULL;
    }
    if ( specification.getType() != accessor->getType() ) {
        reportMissingAccessor(specification, methodName + "() returning " +
            specification.getType());
        return NULL;
    }
    return accessor;
}

const EntityControlAccessor *
GenericEditor::findSetter(const ControlSpecification &specification) const
{
    String methodName = String("set") + specification.getLabel();
    const EntityControlAccessor *accessor =
        entity->getControlAccessor(specification.getName());
    if ( accessor == NULL ||
         specification.getType() != accessor->getType() ) {
        reportMissingAccessor(specification, methodName + "(" +
            specification.getType() + ")");
        return NULL;
    }
    return accessor;
}

void
GenericEditor::reportMissingAccessor(const ControlSpecification &specification,
                                     const String &accessor) const
{
    Logger::reportMessage("GenericEditor", Logger::WARNING, "GenericEditor",
        String("Class ") + entityClassName +
        " declares control \"" + specification.getName() +
        "\" but registers no accessor " + accessor +
        "; register it with addControlSpecification or fix the control " +
        "specification.");
}

String
GenericEditor::getSimpleClassName(const Entity &entity)
{
    return entity.getClassSimpleName();
}
