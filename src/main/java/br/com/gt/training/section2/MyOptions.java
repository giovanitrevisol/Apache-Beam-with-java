package br.com.gt.training;

import org.apache.beam.sdk.options.PipelineOptions;

public interface MyOptions extends PipelineOptions {

    void setInputFile(String file);
    String getInputFile();
    void setOutputFile(String file);
    String getOutputFile();

    void setExtn(String extn);
    String getExtn();



}
