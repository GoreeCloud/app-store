import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

ApplicationWindow {
    id: window
    visible: true
    width: 1080
    height: 760
    minimumWidth: 560
    minimumHeight: 480
    title: "GoreeCloud App Store"
    // Snapshot of canonical GoreeCloud UI semantic colors (gc-ui c833c6c).
    readonly property color canvas: "#F6F7FB"
    readonly property color surface: "#FFFFFF"
    readonly property color ink: "#1B2030"
    readonly property color softInk: "#505B70"
    readonly property color outline: "#CED3DF"
    property var filteredApps: catalogApps

    function refresh() {
        const needle = search.text.toLowerCase().trim()
        const selectedPlatform = platform.currentIndex === 0 ? "" : platform.model[platform.currentIndex].toLowerCase()
        const selectedCategory = category.currentIndex === 0 ? "" : category.model[category.currentIndex]
        const result = []
        for (let i = 0; i < catalogApps.length; i++) {
            const app = catalogApps[i]
            const text = (app.name + " " + app.summary + " " + app.category).toLowerCase()
            if (needle && text.indexOf(needle) < 0) continue
            if (selectedPlatform && app.platforms.indexOf(selectedPlatform) < 0) continue
            if (selectedCategory && app.category !== selectedCategory) continue
            result.push(app)
        }
        result.sort((a, b) => a.name.localeCompare(b.name))
        filteredApps = result
    }

    Component.onCompleted: {
        const cats = {}
        for (let i=0; i<catalogApps.length; i++) cats[catalogApps[i].category] = true
        category.model = ["All categories"].concat(Object.keys(cats).sort())
        refresh()
    }

    Rectangle {
        anchors.fill: parent
        color: window.canvas
    }
    ColumnLayout {
        anchors.fill: parent
        anchors.margins: 28
        spacing: 18

        RowLayout {
            Layout.fillWidth: true
            spacing: 18
            Rectangle {
                width: 52; height: 52; radius: 16
                color: window.ink
                Label { anchors.centerIn: parent; text: "G"; font.bold: true; font.pixelSize: 25; color: window.surface }
            }
            ColumnLayout {
                spacing: 2
                Label { text: "GoreeCloud"; font.bold: true; font.pixelSize: 18; color: window.ink }
                Label { text: "App Store  ·  Native Linux preview"; color: window.softInk }
            }
            Item { Layout.fillWidth: true }
            Label { text: catalogApps.length + " projects"; color: window.softInk }
        }

        Rectangle {
            Layout.fillWidth: true
            implicitHeight: warning.implicitHeight + 24
            radius: 14; color: window.surface
            border.color: window.outline
            Label {
                id: warning
                anchors.left: parent.left; anchors.right: parent.right
                anchors.leftMargin: 20; anchors.rightMargin: 20
                anchors.verticalCenter: parent.verticalCenter
                wrapMode: Text.WordWrap
                text: "Read-only source catalog. Installers and automatic updates are not enabled."
                color: window.softInk
                Accessible.role: Accessible.StaticText
            }
        }

        Label { text: "Explore applications"; font.pixelSize: 28; font.bold: true; color: window.ink }
        RowLayout {
            Layout.fillWidth: true
            spacing: 12
            TextField {
                id: search
                Layout.fillWidth: true
                placeholderText: "Search applications and services"
                Accessible.name: "Search applications"
                onTextChanged: window.refresh()
            }
            ComboBox {
                id: category
                Layout.preferredWidth: 190
                model: ["All categories"]
                Accessible.name: "Category"
                onCurrentIndexChanged: window.refresh()
            }
            ComboBox {
                id: platform
                Layout.preferredWidth: 165
                model: ["All platforms", "Web", "Linux", "Android"]
                Accessible.name: "Platform"
                onCurrentIndexChanged: window.refresh()
            }
        }
        Label {
            text: window.filteredApps.length + " matching projects"
            color: window.softInk
            Accessible.role: Accessible.StaticText
        }
        ListView {
            id: results
            Layout.fillWidth: true
            Layout.fillHeight: true
            clip: true
            spacing: 12
            model: window.filteredApps
            ScrollBar.vertical: ScrollBar {}
            delegate: Rectangle {
                required property var modelData
                width: results.width - 18
                height: 148
                color: window.surface
                radius: 18
                border.color: window.outline
                ColumnLayout {
                    anchors.fill: parent
                    anchors.margins: 18
                    spacing: 5
                    RowLayout {
                        Layout.fillWidth: true
                        Label { text: modelData.name; font.bold: true; font.pixelSize: 18; color: window.ink }
                        Item { Layout.fillWidth: true }
                        Label { text: modelData.category; color: window.softInk }
                    }
                    Label { text: modelData.summary; Layout.fillWidth: true; color: window.softInk; wrapMode: Text.WordWrap }
                    RowLayout {
                        Layout.fillWidth: true
                        Label { text: modelData.platforms.join(" · "); color: window.softInk }
                        Item { Layout.fillWidth: true }
                        Button {
                            text: "View source"
                            Accessible.name: "View source for " + modelData.name
                            onClicked: Qt.openUrlExternally(modelData.source_url)
                        }
                    }
                }
            }
            footer: Item { height: 12 }
        }
    }
}
