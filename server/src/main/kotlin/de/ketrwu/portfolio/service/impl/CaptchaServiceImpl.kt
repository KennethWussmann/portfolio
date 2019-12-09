package de.ketrwu.portfolio.service.impl

import de.ketrwu.portfolio.Slf4j
import de.ketrwu.portfolio.entity.CaptchaForm
import de.ketrwu.portfolio.entity.api.CaptchaReloadRequest
import de.ketrwu.portfolio.entity.api.CaptchaReloadResponse
import de.ketrwu.portfolio.service.CaptchaService
import org.apache.commons.lang.RandomStringUtils
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.Font
import java.awt.FontFormatException
import java.awt.geom.AffineTransform
import java.awt.geom.QuadCurve2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64
import java.util.Random
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.collections.set

/**
 * Service to create captcha images for the frontend and validate captcha answers
 * @author Kenneth Wußmann
 */
@Service
class CaptchaServiceImpl : CaptchaService {

    @Value("classpath:static/captcha-fonts/atwriter.ttf")
    lateinit var ttfFontFile: Resource

    private val solutions = HashMap<String, String>()
    private val fonts = ArrayList<Font>()

    private val randomFont: Font
        get() = fonts[RANDOM.nextInt(fonts.size)]

    /**
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    override fun createCaptcha(captchaForm: CaptchaForm): String {
        val challenge = generateCaptchaChallenge()
        captchaForm.captchaToken = challenge.token
        captchaForm.captchaImage = challenge.image
        return challenge.image
    }

    /**
     * {@inheritDoc}
     */
    override fun checkCaptcha(captchaForm: CaptchaForm): Boolean {
        if (captchaForm.captchaToken?.trim { it <= ' ' }?.isEmpty() != false) {
            return false
        }
        if (captchaForm.captchaResponse?.trim { it <= ' ' }?.isEmpty() != false) {
            return false
        }
        if (captchaForm.captchaToken?.let { solutions[it] == null } == true) {
            return false
        }

        return captchaForm.captchaToken?.let { captchaToken ->
            val solution = solutions[captchaToken]
            solutions.remove(captchaToken)
            captchaForm.captchaResponse?.let {
                solution.equals(it, true)
            }
        } ?: false
    }

    /**
     * {@inheritDoc}
     */
    @EventListener(ApplicationReadyEvent::class)
    override fun loadFonts() {
        fonts.clear()
        try {
            fonts.add(Font("LucidaTypewriterRegular", Font.ITALIC, FONT_SIZE))
        } catch (e: Exception) {
            createFont(ttfFontFile)?.let(fonts::add)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun reloadCaptcha(captchaReloadRequest: CaptchaReloadRequest): CaptchaReloadResponse? {
        return if (solutions.containsKey(captchaReloadRequest.token)) {
            solutions.remove(captchaReloadRequest.token)
            generateCaptchaChallenge(SOLUTION_LENGTH[1])
        } else null
    }

    private fun generateRandomString(length: Int): String {
        return RandomStringUtils.random(
            length,
            true,
            true
        ).toLowerCase()
    }

    @Suppress("MagicNumber")
    private fun generateCaptchaChallenge(
        length: Int = RANDOM.nextInt(SOLUTION_LENGTH[1] - SOLUTION_LENGTH[0] + 1) + SOLUTION_LENGTH[0]
    ): CaptchaReloadResponse {
        val solution = generateRandomString(length)
        val token = UUID.randomUUID().toString()

        solutions[token] = solution

        val img = BufferedImage(IMG_SIZE[0], IMG_SIZE[1], BufferedImage.TYPE_INT_ARGB)
        val g2d = img.createGraphics()
        val solutionBytes = solution.toByteArray()
        g2d.color = Color(0, 0, 0, 100)
        g2d.fillRect(0, 0, IMG_SIZE[0], IMG_SIZE[1])
        g2d.font = randomFont

        val dia = 10
        for (i in dia / 2 until IMG_SIZE[1] step dia) {
            g2d.paint = STRING_COLORS.shuffled().first()
            val curve = QuadCurve2D.Float(
                0f, i.toFloat(),
                (Math.random() * 70).toFloat(), (Math.random() * 60).toFloat(),
                0f + IMG_SIZE[0], i.toFloat()
            )
            g2d.draw(curve)
        }

        g2d.paint = Color.white
        for (i in solutionBytes.indices) {
            AffineTransform().let {
                it.shear(0.1 * i, 0.0)
                g2d.transform = it
            }
            val x = (Math.random() * 10).toInt() + i * 20
            g2d.drawString(
                String(byteArrayOf(solutionBytes[i])),
                x,
                (Math.random() * 10 + 20).toInt()
            )
        }

        g2d.dispose()

        val os = ByteArrayOutputStream()
        ImageIO.write(img, "png", os)

        return CaptchaReloadResponse(token, Base64.getEncoder().encodeToString(os.toByteArray()))
    }

    private fun createFont(resource: Resource): Font? {
        var font: Font? = null
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, resource.inputStream)
            font = font?.deriveFont(FONT_SIZE)
        } catch (e: FontFormatException) {
            log.error("Failed to create font of file \"{}\"", resource.url, e)
        } catch (e: IOException) {
            log.error("Failed to create font of file \"{}\". (file not found?)", resource.url, e)
        }
        return font
    }

    companion object {
        private val RANDOM = Random()
        private val IMG_SIZE = intArrayOf(180, 40)
        private val SOLUTION_LENGTH = intArrayOf(3, 7)
        private val STRING_COLORS = listOf(Color.red, Color.gray, Color.yellow, Color.blue, Color.orange)
        private const val FONT_SIZE = 20

        @Slf4j
        lateinit var log: Logger
    }
}
