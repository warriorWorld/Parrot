package com.harbinger.parrot.director

import com.harbinger.parrot.R
import com.harbinger.parrot.bean.FileBean
import com.harbinger.parrot.bean.FileType
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception

/**
 * Created by acorn on 2020/11/5.
 */
class FileModel : IFileModel {
    override fun getFileList(path: String): ArrayList<FileBean> {
        val f = File(path) //第一级目录 reptile
        if (!f.exists()) {
            throw FileNotFoundException("$path not found!!!")
        }
        if (!f.isDirectory) {
            throw Exception("$path is not a directory!!!")
        }
        val result = ArrayList<FileBean>()
        val files = f.listFiles()
        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                val directory = FileBean(
                    file.absolutePath, file.name, R.drawable.ic_flounder,
                    file.lastModified(), file.length(), FileType.FOLDER
                )
                result.add(directory)
                continue
            }
            if (file.absolutePath.endsWith(".wav")) {
                val record = FileBean(
                    file.absolutePath, file.name,
                    R.drawable.ic_flounder, file.lastModified(), file.length(), FileType.FILE
                )
                result.add(record)
                continue
            }
        }
        result.sortWith(compareBy({ it.fileType }, { it.name }))
        return result
    }
}