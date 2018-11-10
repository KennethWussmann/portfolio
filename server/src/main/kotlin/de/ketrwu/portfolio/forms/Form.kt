package de.ketrwu.portfolio.forms

import de.ketrwu.portfolio.forms.validation.FormToken
import javax.validation.constraints.NotEmpty

/**
 * @author Kenneth Wußmann
 */
@FormToken(message = "Dieses Formular kann nicht erneut abgesendet werden.")
open class Form {

    @NotEmpty
    var formToken: String? = null
}