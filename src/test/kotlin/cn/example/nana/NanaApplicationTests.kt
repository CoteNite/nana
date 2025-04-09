package cn.example.nana

import cn.example.nana.client.WebSearchClient
import cn.example.nana.commons.utils.CrawlUtil
import com.hankcs.hanlp.HanLP
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest


@SpringBootTest
class NanaApplicationTests{

    private val webSearchClient = WebSearchClient("http://localhost:8041/")

    @Test
    fun SearchTest() {

        val keywordList = HanLP.extractKeyword("""
            ### 镜像阶段的作用总结

            镜像阶段（Mirror Stage）是法国精神分析学家雅克·拉康（Jacques Lacan）提出的核心理论，描述了婴儿在6个月至18个月期间通过镜像（或他人的反馈）形成自我认知的过程。以下是镜像阶段的主要作用：

            1. **自我认同的初步形成**  
               - 婴儿通过镜中的形象首次认识到自己是一个完整的统一体，尽管此时其身体协调能力尚未发育完全。这种认同被称为“理想我”（Ideal-I），是一种虚构的、完整的自我形象。
               - 镜像阶段标志着婴儿从“破碎的身体”感知（未整合的肢体体验）向“整体性”幻想的过渡。

            2. **主体性的建构**  
               - 镜像阶段是主体（自我）形成的起点，婴儿通过对外部形象（镜像或他人姿态）的认同，开始构建自我身份。这一过程依赖于“小他者”（他人或镜像）的反馈。
               - 拉康认为，自我本质上是异化的，因为它是通过外部形象（他者）的误认（misrecognition）构建的，而非真实的自我。

            3. **想象界的进入**  
               - 镜像阶段属于拉康理论中的“想象界”（Imaginary Order），即通过形象和幻觉构建的心理领域。婴儿对镜像的迷恋是一种自恋式的认同，为后续进入“象征界”（语言和社会秩序）奠定基础。
               - 想象界的自我具有虚幻性，因为它掩盖了主体内部的破碎和匮乏。

            4. **异化与悲剧性**  
               - 镜像阶段的认同是一种异化过程：婴儿将镜像中的完美形象误认为真实的自己，导致主体永远无法与自我完全重合。这种异化是精神分裂、自恋等心理问题的根源。
               - 拉康称之为“一出悲剧”，因为主体终其一生都在追求这种虚幻的统一性。

            5. **对社会化和语言的影响**  
               - 镜像阶段为婴儿后续接受社会规范（象征界）和语言结构提供了心理基础。通过与他者的互动，主体逐渐被社会化和符号化。

            ### 理论意义
            - **批判传统哲学**：拉康反对笛卡尔式的“我思”哲学，强调自我是后天构建的幻象，而非先天存在的实体。
            - **精神分析的革新**：镜像阶段理论修正了弗洛伊德的自我概念，提出自我是“他者的产物”（“我是个他者”）。
            - **文化与社会应用**：该理论被广泛用于文学、电影分析（如《鸟人》），揭示主体如何通过虚构形象构建身份。

            ### 关键引用
            - “镜像阶段是一出戏剧，其内在动力从不足（破碎的身体）奔向预期（完整的幻象）。”  
            - “自我是一个他者。”——拉康引用诗人兰波的话，强调自我的异化本质。

            镜像阶段不仅是婴儿心理发展的关键节点，也是理解人类主体性、异化和社会化的重要框架。
        """.trimIndent(), 3)
        println(keywordList.toList())

    }



}




