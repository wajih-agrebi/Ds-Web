from docx import Document
from docx.shared import Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.text.paragraph import Paragraph
from pathlib import Path

path = Path(r'C:\LIM 3\WEB\ds\DriveEasy_Project_Report_Updated.docx')
image_path = Path(r'C:\LIM 3\WEB\ds\dashboard-updated.png')
doc = Document(path)

# Fix a clear typographical error in the cover metadata.
for table in doc.tables:
    for row in table.rows:
        if len(row.cells) > 1 and row.cells[0].text.strip() == 'Academic year':
            row.cells[1].text = '2026–2027'

# Update the report narrative so it matches the current UI and supplied screenshot.
replacements = {
    'The supplied evidence includes populated fleet and client screens, data-entry forms, successful eligibility, price simulation, reservation, and statistics responses, plus two 409 responses rejecting an early start and a return without an active rental. The supplied captures do not show Eureka registrations or a Zipkin trace. Some dashboard and rental-list images show loading states; these are recorded as evidence gaps.':
    'The supplied evidence includes a populated fleet and client register, data-entry forms, a loaded agency dashboard, successful eligibility, price simulation, reservation, and statistics responses, plus two 409 responses rejecting an early start and a return without an active rental. The dashboard capture shows three vehicles (two available and one in maintenance), no active rentals, and zero revenue for the displayed sample. Eureka registrations and a Zipkin trace are not shown. The supplied rental-list capture still shows a loading state, so it does not confirm that page completed loading.',
    'The frontend uses Angular 22 standalone components and Router. DriveEasyApi centralizes calls under http://localhost:8080/api; the browser does not call services directly. Routes exist for dashboard, vehicle list/create/edit, client list/create/detail, rental list/new, and vehicle return. The shell has persistent agency navigation.':
    'The frontend uses Angular 22 standalone components and Router. DriveEasyApi centralizes calls under http://localhost:8080/api; the browser does not call services directly. Routes exist for dashboard, vehicle list/create/edit, client list/create/edit/detail, rental list/new, and vehicle return. The shell has persistent agency navigation. Rental tables display client names and vehicle make/model when the related records load, with IDs retained as a fallback. The vehicle form accepts either an image URL or a local JPG, PNG, or WebP file up to 2 MB; selected files are previewed and stored in the vehicle imageUrl field. The application keeps this data in memory, so it resets when the service restarts.',
    'Supplied populated images show vehicle cards and filters, a client register with tier and licence indicators, and vehicle/client data-entry forms. The blank Add Client image proves the form view exists but does not show a successful save. The supplied dashboard and rental list images show loading placeholders and do not prove data loaded successfully.':
    'Supplied populated images show vehicle cards and filters, a client register with tier and licence indicators, vehicle/client data-entry forms, and the loaded dashboard shown in Figure 7. The blank Add Client image proves the form view exists but does not show a successful save. The dashboard screenshot confirms that the overview data loaded for the captured sample; its zero revenue and rental values should not be read as a broader performance result. The supplied rental list image still shows a loading placeholder and does not prove that page completed loading.',
    '[Insert screenshots: loaded dashboard and loaded rental list.] The supplied views show Loading.':
    'The supplied dashboard screenshot shows loaded data (Figure 7). [Insert screenshot: rental list after it has finished loading.] The available rental-list capture still shows Loading.',
    'Dashboard and rental-list captures show loading rather than completed state.':
    'The loaded dashboard is documented in Figure 7. The supplied rental-list capture still shows a loading state.',
    'Capture successful client save, loaded dashboard, rental wizard, rental list, departure, return, and printable return summary.':
    'Capture successful client save, the rental wizard, a loaded rental list, departure, return, and the return summary.'
}
for p in doc.paragraphs:
    if p.text in replacements:
        p.text = replacements[p.text]

# Shift the existing figure list and captions so the new dashboard image is Figure 7.
import re
fig_start = next(i for i,p in enumerate(doc.paragraphs) if p.text.strip() == 'Figures')
tables_start = next(i for i,p in enumerate(doc.paragraphs) if p.text.strip() == 'Tables')
for p in doc.paragraphs[fig_start + 1:tables_start]:
    m = re.match(r'^(\s*)(\d+)(\..*)$', p.text)
    if m and int(m.group(2)) >= 7:
        p.text = f'{m.group(1)}{int(m.group(2)) + 1}{m.group(3)}'
for p in doc.paragraphs:
    m = re.match(r'^Figure (\d+)\.(.*)$', p.text)
    if m and int(m.group(1)) >= 7:
        p.text = f'Figure {int(m.group(1)) + 1}.{m.group(2)}'
# Insert screenshot and caption after the existing Add-vehicle figure.
anchor = next(p for p in doc.paragraphs if p.text.startswith('Figure 6. Add-vehicle form'))
image_p = OxmlElement('w:p')
anchor._p.addnext(image_p)
image_para = Paragraph(image_p, anchor._parent)
image_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = image_para.add_run()
shape = run.add_picture(str(image_path), width=Inches(6.45))
shape._inline.docPr.set('descr', 'Agency overview dashboard showing fleet, rental, revenue, occupancy, and category statistics for the captured sample.')
caption_p = OxmlElement('w:p')
image_para._p.addnext(caption_p)
caption = Paragraph(caption_p, anchor._parent)
caption.style = doc.styles['Caption']
caption.alignment = WD_ALIGN_PARAGRAPH.CENTER
caption.add_run('Figure 7. Loaded agency dashboard showing the captured fleet and rental statistics.')

# Include the new figure in the manually maintained contents list.
list_anchor = next(p for p in doc.paragraphs if p.text.strip().startswith('8. Client eligibility response'))
new_p = OxmlElement('w:p')
list_anchor._p.addprevious(new_p)
new_list_p = Paragraph(new_p, list_anchor._parent)
new_list_p.style = doc.styles['Normal']
new_list_p.add_run('7. Loaded agency dashboard ..................................... 8')

# Add current dashboard and feature behavior to the evidence gaps section without overstating tests.
doc.save(path)
print(path)




