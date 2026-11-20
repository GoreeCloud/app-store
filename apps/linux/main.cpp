#include <QFile>
#include <QGuiApplication>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QVariantList>

int main(int argc, char *argv[]) {
    QGuiApplication app(argc, argv);
    app.setApplicationName("GoreeCloud App Store");
    app.setOrganizationName("GoreeCloud");

    QFile file(":/qt/qml/GoreeCloud/Store/catalog.json");
    if (!file.open(QIODevice::ReadOnly)) {
        qCritical("Unable to load bundled GoreeCloud App Store catalog");
        return 2;
    }
    QJsonParseError error;
    const QJsonDocument document = QJsonDocument::fromJson(file.readAll(), &error);
    if (error.error != QJsonParseError::NoError || !document.isObject()
        || !document.object().value("apps").isArray()) {
        qCritical("Invalid bundled catalog JSON");
        return 3;
    }
    QVariantList apps;
    for (const auto &item : document.object().value("apps").toArray()) {
        if (!item.isObject()) {
            qCritical("Non-object catalog entry");
            return 4;
        }
        apps.append(item.toObject().toVariantMap());
    }

    QQmlApplicationEngine engine;
    engine.rootContext()->setContextProperty("catalogApps", apps);
    engine.loadFromModule("GoreeCloud.Store", "Main");
    if (engine.rootObjects().isEmpty()) return 5;
    return app.exec();
}
