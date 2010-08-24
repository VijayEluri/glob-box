package org.esa.beam.dataio.envi;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EnviProductReaderPlugIn implements ProductReaderPlugIn {

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (input instanceof ImageInputStream) {
            return checkDecodeQualificationOnStream((ImageInputStream) input);
        } else {
            return checkDecodeQualificationOnFile(new File(input.toString()));
        }
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{EnviConstants.FORMAT_NAME};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new EnviProductReader(this);
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".hdr", ".zip"};
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public String getDescription(Locale locale) {
        return EnviConstants.DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    static File getInputFile(Object input) {
        return new File(input.toString());
    }


    static boolean isCompressedFile(File file) {
        return file.getPath().lastIndexOf("." + EnviConstants.ZIP) > -1;
    }

    static InputStream getHeaderStreamFromZip(ZipFile productZip) throws IOException {
        final Enumeration entries = productZip.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            final String name = zipEntry.getName();
            if (name.indexOf(".hdr") > 0) {
                return productZip.getInputStream(zipEntry);
            }
        }
        return null;
    }

    private static DecodeQualification checkDecodeQualificationOnStream(InputStream headerStream) {
        return checkDecodeQualificationOnStream(new MemoryCacheImageInputStream(headerStream));
    }

    private static DecodeQualification checkDecodeQualificationOnStream(ImageInputStream headerStream) {
        try {
            final String line = headerStream.readLine();
            if (line != null && line.startsWith(EnviConstants.FIRST_LINE)) {
                return DecodeQualification.SUITABLE;
            }

        } catch (IOException ignore) {
            // intentionally nothing in here tb 20080409
        }
        return DecodeQualification.UNABLE;
    }


    private static DecodeQualification checkDecodeQualificationOnFile(File headerFile) {
        try {
            if (isCompressedFile(headerFile)) {
                final ZipFile productZip = new ZipFile(headerFile, ZipFile.OPEN_READ);

                if (productZip.size() != 2) {
                    productZip.close();
                    return DecodeQualification.UNABLE;
                }

                final InputStream headerStream = getHeaderStreamFromZip(productZip);
                if (headerStream != null) {
                    final DecodeQualification result = checkDecodeQualificationOnStream(headerStream);
                    productZip.close();
                    return result;
                }
                productZip.close();
            } else {
                ImageInputStream headerStream = new FileImageInputStream(headerFile);
                final DecodeQualification result = checkDecodeQualificationOnStream(headerStream);
                headerStream.close();
                return result;
            }
        }
        catch (IOException ignored) {
            // intentionally left empty - returns the same as the line below tb 20080409
        }
        return DecodeQualification.UNABLE;
    }
}
