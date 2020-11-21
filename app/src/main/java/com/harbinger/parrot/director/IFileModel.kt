package com.harbinger.parrot.director

import com.harbinger.parrot.bean.FileBean

/**
 * Created by acorn on 2020/11/4.
 */
interface IFileModel {
    fun getFileList(path: String): List<FileBean>
}