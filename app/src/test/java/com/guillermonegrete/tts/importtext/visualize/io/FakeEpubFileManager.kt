package com.guillermonegrete.tts.importtext.visualize.io

import java.io.File

class FakeEpubFileManager: EpubFileManager {
    override val rootDir = File("root")
    override val filesDir = File(rootDir, "files")
}